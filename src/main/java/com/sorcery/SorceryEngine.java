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
        rules = gson.fromJson(new FileReader(dataDir.resolve("council-rules.json").toFile()), Model.CouncilRules.class);
       Model.ApplicationsContainer container = gson.fromJson(new FileReader(dataDir.resolve("applications.json").toFile()), Model.ApplicationsContainer.class);
        applications = container.getApplications();
    }

  public List<Model.StudentResult> process() {
        List<Model.StudentResult> results = new ArrayList<>();

        for (Model.Application app : applications) {
            Model.StudentResult res = new Model.StudentResult(app);
            String fullName = app.getFirstName() + " " + app.getFamilyName();
            boolean isInvited = rules.getInvitations() != null && rules.getInvitations().contains(fullName);

            if (isInvited) {
                res.setAccepted(true);
                res.setScore(calculateScore(app));
            } else {
                String vetoReason = checkVetoes(app);
                if (vetoReason != null) {
                    res.setAccepted(false);
                    res.setRejectionReason(vetoReason);
                } else {
                    res.setAccepted(true);
                    res.setScore(calculateScore(app));
                }
            }
            results.add(res);
        }

        List<Model.StudentResult> accepted = new ArrayList<>();
        List<Model.StudentResult> vetoed = new ArrayList<>();

        for (Model.StudentResult r : results) {
            if (r.isAccepted()) {
                accepted.add(r);
            } else {
                vetoed.add(r);
            }
        }

        // Ordenar candidatos aptos: Score desc, Edad asc, Apellido asc, Nombre asc, ID asc
        accepted.sort((a, b) -> {
            if (b.getScore() != a.getScore()) {
                return Integer.compare(b.getScore(), a.getScore());
            }
            if (a.getApplication().getAge() != b.getApplication().getAge()) {
                return Integer.compare(a.getApplication().getAge(), b.getApplication().getAge());
            }
            int familyComp = a.getApplication().getFamilyName().compareTo(b.getApplication().getFamilyName());
            if (familyComp != 0) {
                return familyComp;
            }
            int nameComp = a.getApplication().getFirstName().compareTo(b.getApplication().getFirstName());
            if (nameComp != 0) {
                return nameComp;
            }
            return a.getApplication().getId().compareTo(b.getApplication().getId());
        });

        // Ordenar vetados por ID para asegurar 100% determinismo sin importar el orden de entrada
        vetoed.sort((a, b) -> a.getApplication().getId().compareTo(b.getApplication().getId()));

        int placesLimit = rules.getPlaces();
        List<Model.StudentResult> finalOutput = new ArrayList<>();
        List<Model.StudentResult> queueRejected = new ArrayList<>();

        for (int i = 0; i < accepted.size(); i++) {
            Model.StudentResult student = accepted.get(i);
            if (i < placesLimit) {
                student.setRank(i + 1);
                student.setHouse(assignHouse(student.getApplication()));
                finalOutput.add(student);
            } else {
                student.setAccepted(false);
                student.setRank(0);
                student.setRejectionReason(String.valueOf(i + 1));
                queueRejected.add(student);
            }
        }

        // Añadir primero los rechazados por falta de plazas ordenados por su ranking, y luego los vetados
        finalOutput.addAll(queueRejected);
        finalOutput.addAll(vetoed);
        return finalOutput;
    }

    private String checkVetoes(Model.Application app) {
        // 1. Familia baneada
        if (rules.getBannedFamilies() != null && rules.getBannedFamilies().contains(app.getFamilyName())) {
            return "Banned family";
        }
        // 2. Rango de edad
        if (app.getAge() < rules.getAgeRange().getMin() || app.getAge() > rules.getAgeRange().getMax()) {
            return "Out of age range";
        }
        // 3. Debilidad inaceptable
        if (rules.getUnacceptableWeaknesses() != null && rules.getUnacceptableWeaknesses().contains(app.getWeakness())) {
            return "Unacceptable weakness";
        }
        // 4. Fechas de aplicación
        LocalDate appDate = LocalDate.parse(app.getApplicationDate());
        LocalDate startDate = LocalDate.parse(rules.getApplicationDates().get("from"));
        LocalDate endDate = LocalDate.parse(rules.getApplicationDates().get("to"));
        if (appDate.isBefore(startDate) || appDate.isAfter(endDate)) {
            return "Outside application dates";
        }
        return null;
    }

    private int calculateScore(Model.Application app) {
        int score = 0;
        Model.PointsConfig pts = rules.getPoints();

        // Virtud
        if (pts.getVirtue() != null && pts.getVirtue().containsKey(app.getVirtue())) {
            score += pts.getVirtue().get(app.getVirtue());
        }
        // Familia
        if (pts.getFamily() != null && pts.getFamily().containsKey(app.getFamilyName())) {
            score += pts.getFamily().get(app.getFamilyName());
        }
        // Debilidad
        if (pts.getWeakness() != null && pts.getWeakness().containsKey(app.getWeakness())) {
            score += pts.getWeakness().get(app.getWeakness()); // Resta puntos (valores negativos)
        }
        // Edad (rango)
        if (pts.getAge() != null) {
            for (Model.AgePointRule arp : pts.getAge()) {
                if (app.getAge() >= arp.getFrom() && app.getAge() <= arp.getTo()) {
                    score += arp.getPoints();
                    break;
                }
            }
        }
        return score;
    }

    private String assignHouse(Model.Application app) {
        String bestHouse = null;
        int maxHouseScore = Integer.MIN_VALUE;

        // Evaluar cada casa en el orden estricto del fichero de reglas
        for (Model.HouseConfig house : rules.getHouses()) {
            int houseScore = 0;
            Model.PointsConfig hPts = house.getPoints();

            if (hPts != null) {
                if (hPts.getVirtue() != null && hPts.getVirtue().containsKey(app.getVirtue())) {
                    houseScore += hPts.getVirtue().get(app.getVirtue());
                }
                if (hPts.getWeakness() != null && hPts.getWeakness().containsKey(app.getWeakness())) {
                    houseScore += hPts.getWeakness().get(app.getWeakness());
                }
                if (hPts.getFamily() != null && hPts.getFamily().containsKey(app.getFamilyName())) {
                    houseScore += hPts.getFamily().get(app.getFamilyName());
                }
            }

            if (houseScore > maxHouseScore) {
                maxHouseScore = houseScore;
                bestHouse = house.getName();
            }
        }
        return bestHouse;
    }
}