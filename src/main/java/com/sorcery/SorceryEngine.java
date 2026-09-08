
package com.sorcery;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public class SorceryEngine {

    private Model.CouncilRules rules;
    private List<Model.Application> applications;

    public void loadData(Path dataDir) throws IOException {
        Gson gson = new Gson();

        rules = gson.fromJson(
                new FileReader(dataDir.resolve("council-rules.json").toFile()),
                Model.CouncilRules.class
        );

        Model.ApplicationsContainer container = gson.fromJson(
                new FileReader(dataDir.resolve("applications.json").toFile()),
                Model.ApplicationsContainer.class
        );

        applications = container.getApplications();
    }

    public List<Model.StudentResult> process() {

        // Aplicaciones que participan en el ranking:
        // candidatos que pasan los vetos + invitados.
        List<Model.StudentResult> ranked = new ArrayList<>();

        // Aplicaciones rechazadas inmediatamente por un veto.
        List<Model.StudentResult> vetoed = new ArrayList<>();

        /*
         * PASO 1
         * Procesar aplicaciones.
         *
         * Los invitados ignoran completamente los vetos.
         * Los demás deben superarlos para participar en el ranking.
         */
        for (Model.Application app : applications) {

            Model.StudentResult result = new Model.StudentResult(app);

            if (isInvited(app)) {
                result.setScore(calculateScore(app));
                ranked.add(result);
                continue;
            }

            String vetoReason = checkVetoes(app);

            if (vetoReason != null) {
                result.setAccepted(false);
                result.setRejectionReason(vetoReason);
                vetoed.add(result);
            } else {
                result.setScore(calculateScore(app));
                ranked.add(result);
            }
        }

        /*
         * PASO 2
         * Crear el ranking REAL.
         *
         * Los invitados no reciben una posición especial.
         * Se ordenan exactamente igual que el resto.
         */
        ranked.sort(this::compareByRankingRules);

        /*
         * PASO 3
         * Asignar una posición a TODOS los participantes del ranking.
         */
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setRank(i + 1);
        }

        /*
         * PASO 4
         * Las invitaciones tienen plaza garantizada y consumen
         * una de las plazas disponibles.
         */
        int placesLimit = rules.getPlaces();

        long invitedCount = ranked.stream()
                .filter(r -> isInvited(r.getApplication()))
                .count();

        /*
         * En unos datos válidos debería haber como máximo tantas
         * invitaciones como plazas.
         */
        if (invitedCount > placesLimit) {
            throw new IllegalStateException(
                    "There are more invited applicants than available places."
            );
        }

        int normalPlacesAvailable = placesLimit - (int) invitedCount;

        /*
         * PASO 5
         * Admitir:
         *
         * - TODOS los invitados.
         * - Los mejores candidatos normales hasta completar
         *   las plazas restantes.
         */
        int normalApplicantsAccepted = 0;

        for (Model.StudentResult result : ranked) {

            if (isInvited(result.getApplication())) {

                result.setAccepted(true);
                result.setHouse(assignHouse(result.getApplication()));

            } else if (normalApplicantsAccepted < normalPlacesAvailable) {

                result.setAccepted(true);
                result.setHouse(assignHouse(result.getApplication()));
                normalApplicantsAccepted++;

            } else {

                result.setAccepted(false);

                /*
                 * El enunciado pide que para los rechazados por falta
                 * de plazas se informe de su posición.
                 */
                result.setRejectionReason(
                        String.valueOf(result.getRank())
                );
            }
        }

        /*
         * PASO 6
         * Los vetados no participan en el ranking.
         *
         * Los ordenamos por ID para que el resultado sea determinista
         * aunque se cambie el orden del fichero de entrada.
         */
        vetoed.sort(
                Comparator.comparing(
                        r -> r.getApplication().getId()
                )
        );

        /*
         * PASO 7
         * Resultado final.
         *
         * Primero aparece el ranking completo de las aplicaciones
         * que llegaron a la fase de puntuación.
         *
         * Después aparecen los vetados.
         */
        List<Model.StudentResult> finalOutput = new ArrayList<>();

        finalOutput.addAll(ranked);
        finalOutput.addAll(vetoed);

        return finalOutput;
    }

    /**
     * Comprueba si una persona fue invitada personalmente
     * por el headmaster.
     */
    private boolean isInvited(Model.Application app) {

        if (rules.getInvitations() == null) {
            return false;
        }

        String fullName =
                app.getFirstName() + " " + app.getFamilyName();

        return rules.getInvitations().contains(fullName);
    }

    /**
     * Comparador del ranking.
     *
     * Orden:
     * 1. Score más alto.
     * 2. Menor edad.
     * 3. Apellido alfabéticamente.
     * 4. Nombre alfabéticamente.
     * 5. ID como desempate final para garantizar determinismo.
     */
    private int compareByRankingRules(
            Model.StudentResult a,
            Model.StudentResult b) {

        // 1. Score descendente
        int scoreComparison =
                Integer.compare(b.getScore(), a.getScore());

        if (scoreComparison != 0) {
            return scoreComparison;
        }

        // 2. Más joven primero
        int ageComparison =
                Integer.compare(
                        a.getApplication().getAge(),
                        b.getApplication().getAge()
                );

        if (ageComparison != 0) {
            return ageComparison;
        }

        // 3. Apellido
        int familyComparison =
                a.getApplication()
                        .getFamilyName()
                        .compareTo(
                                b.getApplication().getFamilyName()
                        );

        if (familyComparison != 0) {
            return familyComparison;
        }

        // 4. Nombre
        int firstNameComparison =
                a.getApplication()
                        .getFirstName()
                        .compareTo(
                                b.getApplication().getFirstName()
                        );

        if (firstNameComparison != 0) {
            return firstNameComparison;
        }

        /*
         * 5. ID.
         *
         * El PDF no necesita este desempate en circunstancias normales,
         * pero evita depender del orden del JSON si dos solicitudes
         * fueran idénticas en todos los criterios anteriores.
         */
        return a.getApplication()
                .getId()
                .compareTo(
                        b.getApplication().getId()
                );
    }

    /**
     * Comprueba los vetos EN EL ORDEN indicado por el council.
     *
     * Si se cumplen varios, solamente se devuelve el primero.
     */
    private String checkVetoes(Model.Application app) {

        // 1. Familia prohibida
        if (rules.getBannedFamilies() != null
                && rules.getBannedFamilies()
                        .contains(app.getFamilyName())) {

            return "Banned family";
        }

        // 2. Fuera del rango de edad
        if (app.getAge() < rules.getAgeRange().getMin()
                || app.getAge() > rules.getAgeRange().getMax()) {

            return "Out of age range";
        }

        // 3. Debilidad no aceptada
        if (rules.getUnacceptableWeaknesses() != null
                && rules.getUnacceptableWeaknesses()
                        .contains(app.getWeakness())) {

            return "Unacceptable weakness";
        }

        // 4. Fuera de las fechas de solicitud
        LocalDate applicationDate =
                LocalDate.parse(app.getApplicationDate());

        LocalDate startDate =
                LocalDate.parse(
                        rules.getApplicationDates().get("from")
                );

        LocalDate endDate =
                LocalDate.parse(
                        rules.getApplicationDates().get("to")
                );

        if (applicationDate.isBefore(startDate)
                || applicationDate.isAfter(endDate)) {

            return "Outside application dates";
        }

        return null;
    }

    /**
     * Calcula exclusivamente la puntuación de admisión.
     */
    private int calculateScore(Model.Application app) {

        int score = 0;

        Model.PointsConfig points = rules.getPoints();

        // Virtud
        if (points.getVirtue() != null) {
            score += points.getVirtue()
                    .getOrDefault(app.getVirtue(), 0);
        }

        // Familia
        if (points.getFamily() != null) {
            score += points.getFamily()
                    .getOrDefault(app.getFamilyName(), 0);
        }

        // Debilidad
        if (points.getWeakness() != null) {
            score += points.getWeakness()
                    .getOrDefault(app.getWeakness(), 0);
        }

        // Edad
        if (points.getAge() != null) {

            for (Model.AgePointRule ageRule : points.getAge()) {

                if (app.getAge() >= ageRule.getFrom()
                        && app.getAge() <= ageRule.getTo()) {

                    score += ageRule.getPoints();
                    break;
                }
            }
        }

        return score;
    }

    /**
     * Calcula la casa de un alumno admitido.
     *
     * Este cálculo es totalmente independiente del score
     * utilizado para el ranking.
     */
    private String assignHouse(Model.Application app) {

        String bestHouse = null;

        int bestHouseScore = Integer.MIN_VALUE;

        /*
         * Es fundamental mantener el orden del JSON.
         *
         * Como solamente sustituimos bestHouse cuando encontramos
         * una puntuación ESTRICTAMENTE mayor, un empate mantiene
         * la primera casa del fichero.
         */
        for (Model.HouseConfig house : rules.getHouses()) {

            int houseScore = 0;

            Model.PointsConfig housePoints =
                    house.getPoints();

            if (housePoints != null) {

                // Virtud
                if (housePoints.getVirtue() != null) {
                    houseScore += housePoints.getVirtue()
                            .getOrDefault(app.getVirtue(), 0);
                }

                // Debilidad
                if (housePoints.getWeakness() != null) {
                    houseScore += housePoints.getWeakness()
                            .getOrDefault(app.getWeakness(), 0);
                }

                // Familia
                if (housePoints.getFamily() != null) {
                    houseScore += housePoints.getFamily()
                            .getOrDefault(app.getFamilyName(), 0);
                }
            }

            if (houseScore > bestHouseScore) {
                bestHouseScore = houseScore;
                bestHouse = house.getName();
            }
        }

        return bestHouse;
    }
}

