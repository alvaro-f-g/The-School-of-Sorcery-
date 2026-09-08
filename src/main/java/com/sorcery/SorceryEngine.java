
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
        List<Model.StudentResult> invited = new ArrayList<>();
        List<Model.StudentResult> candidates = new ArrayList<>();
        List<Model.StudentResult> vetoed = new ArrayList<>();

        /*
         * First pass:
         * - Invitations are guaranteed a place and override all vetoes.
         * - Non-invited applications go through the veto checks.
         */
        for (Model.Application app : applications) {
            Model.StudentResult res = new Model.StudentResult(app);

            String fullName = app.getFirstName() + " " + app.getFamilyName();

            boolean isInvited =
                    rules.getInvitations() != null
                    && rules.getInvitations().contains(fullName);

            if (isInvited) {
                res.setAccepted(true);
                res.setScore(calculateScore(app));
                invited.add(res);
                continue;
            }

            String vetoReason = checkVetoes(app);

            if (vetoReason != null) {
                res.setAccepted(false);
                res.setRejectionReason(vetoReason);
                vetoed.add(res);
            } else {
                res.setAccepted(true);
                res.setScore(calculateScore(app));
                candidates.add(res);
            }
        }

        /*
         * Invitations are guaranteed places.
         *
         * Their order must also be deterministic, so we sort them using
         * the same ranking rules plus the application ID as final tie-breaker.
         */
        sortByRanking(invited);

        /*
         * Normal candidates are ranked by:
         * 1. Score descending
         * 2. Age ascending
         * 3. Family name alphabetically
         * 4. First name alphabetically
         * 5. Application ID as final deterministic tie-breaker
         */
        sortByRanking(candidates);

        /*
         * The invited students consume places first.
         */
        int placesLimit = rules.getPlaces();

        List<Model.StudentResult> admitted = new ArrayList<>();
        List<Model.StudentResult> rejectedByPosition = new ArrayList<>();

        int placesUsed = 0;

        for (Model.StudentResult student : invited) {
            if (placesUsed < placesLimit) {
                student.setAccepted(true);
                student.setRank(++placesUsed);
                student.setHouse(assignHouse(student.getApplication()));
                admitted.add(student);
            } else {
                /*
                 * This should only happen if the rules contain more
                 * invitations than available places.
                 */
                student.setAccepted(false);
                student.setRank(placesUsed + 1);
                student.setRejectionReason(String.valueOf(student.getRank()));
                rejectedByPosition.add(student);
            }
        }

        /*
         * Fill the remaining places with the best normal candidates.
         */
        for (Model.StudentResult student : candidates) {
            if (placesUsed < placesLimit) {
                student.setAccepted(true);
                student.setRank(++placesUsed);
                student.setHouse(assignHouse(student.getApplication()));
                admitted.add(student);
            } else {
                student.setAccepted(false);

                /*
                 * Rank is the student's actual position in the complete
                 * admission ranking.
                 */
                student.setRank(placesUsed + rejectedByPosition.size() + 1);
                student.setRejectionReason(String.valueOf(student.getRank()));

                rejectedByPosition.add(student);
            }
        }

        /*
         * The vetoed applications do not participate in the ranking.
         * Their rejection reason is the veto itself.
         *
         * Sort by ID to guarantee deterministic output regardless
         * of the input application order.
         */
        vetoed.sort(
                Comparator.comparing(
                        r -> r.getApplication().getId()
                )
        );

        /*
         * Final output:
         * 1. Admitted students
         * 2. Students rejected because there were no places
         * 3. Students rejected by veto
         */
        List<Model.StudentResult> finalOutput = new ArrayList<>();

        finalOutput.addAll(admitted);
        finalOutput.addAll(rejectedByPosition);
        finalOutput.addAll(vetoed);

        return finalOutput;
    }

    /**
     * Sort applications according to the council ranking rules.
     */
    private void sortByRanking(List<Model.StudentResult> students) {
        students.sort((a, b) -> {

            // 1. Score: highest first
            if (a.getScore() != b.getScore()) {
                return Integer.compare(b.getScore(), a.getScore());
            }

            // 2. Age: youngest first
            if (a.getApplication().getAge() != b.getApplication().getAge()) {
                return Integer.compare(
                        a.getApplication().getAge(),
                        b.getApplication().getAge()
                );
            }

            // 3. Family name: alphabetical
            int familyComp = a.getApplication()
                    .getFamilyName()
                    .compareTo(b.getApplication().getFamilyName());

            if (familyComp != 0) {
                return familyComp;
            }

            // 4. First name: alphabetical
            int nameComp = a.getApplication()
                    .getFirstName()
                    .compareTo(b.getApplication().getFirstName());

            if (nameComp != 0) {
                return nameComp;
            }

            // 5. ID: deterministic final tie-breaker
            return a.getApplication()
                    .getId()
                    .compareTo(b.getApplication().getId());
        });
    }

    private String checkVetoes(Model.Application app) {

        // 1. Banned family
        if (rules.getBannedFamilies() != null
                && rules.getBannedFamilies().contains(app.getFamilyName())) {

            return "Banned family";
        }

        // 2. Age range
        if (app.getAge() < rules.getAgeRange().getMin()
                || app.getAge() > rules.getAgeRange().getMax()) {

            return "Out of age range";
        }

        // 3. Unacceptable weakness
        if (rules.getUnacceptableWeaknesses() != null
                && rules.getUnacceptableWeaknesses().contains(app.getWeakness())) {

            return "Unacceptable weakness";
        }

        // 4. Application dates
        LocalDate appDate = LocalDate.parse(app.getApplicationDate());

        LocalDate startDate = LocalDate.parse(
                rules.getApplicationDates().get("from")
        );

        LocalDate endDate = LocalDate.parse(
                rules.getApplicationDates().get("to")
        );

        if (appDate.isBefore(startDate)
                || appDate.isAfter(endDate)) {

            return "Outside application dates";
        }

        return null;
    }

    private int calculateScore(Model.Application app) {

        int score = 0;
        Model.PointsConfig pts = rules.getPoints();

        // Virtue
        if (pts.getVirtue() != null
                && pts.getVirtue().containsKey(app.getVirtue())) {

            score += pts.getVirtue().get(app.getVirtue());
        }

        // Family
        if (pts.getFamily() != null
                && pts.getFamily().containsKey(app.getFamilyName())) {

            score += pts.getFamily().get(app.getFamilyName());
        }

        // Weakness
        if (pts.getWeakness() != null
                && pts.getWeakness().containsKey(app.getWeakness())) {

            score += pts.getWeakness().get(app.getWeakness());
        }

        // Age
        if (pts.getAge() != null) {

            for (Model.AgePointRule arp : pts.getAge()) {

                if (app.getAge() >= arp.getFrom()
                        && app.getAge() <= arp.getTo()) {

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

        /*
         * Houses are evaluated in exactly the order in which they
         * appear in council-rules.json.
         *
         * Using '>' instead of '>=' means that ties go to the
         * house appearing first in the rules file.
         */
        for (Model.HouseConfig house : rules.getHouses()) {

            int houseScore = 0;
            Model.PointsConfig hPts = house.getPoints();

            if (hPts != null) {

                if (hPts.getVirtue() != null
                        && hPts.getVirtue().containsKey(app.getVirtue())) {

                    houseScore += hPts.getVirtue().get(app.getVirtue());
                }

                if (hPts.getWeakness() != null
                        && hPts.getWeakness().containsKey(app.getWeakness())) {

                    houseScore += hPts.getWeakness().get(app.getWeakness());
                }

                if (hPts.getFamily() != null
                        && hPts.getFamily().containsKey(app.getFamilyName())) {

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

