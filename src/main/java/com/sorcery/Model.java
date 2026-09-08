package com.sorcery;

import java.util.List;
import java.util.Map;

public class Model {

    public static class Application {
        private String id;
        private String firstName;
        private String familyName;
        private int age;
        private String virtue;
        private String weakness;
        private String applicationDate;

        // Getters y Setters
        public String getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getFamilyName() { return familyName; }
        public int getAge() { return age; }
        public String getVirtue() { return virtue; }
        public String getWeakness() { return weakness; }
        public String getApplicationDate() { return applicationDate; }
    }

    public static class AgeRange {
        private int min;
        private int max;
        public int getMin() { return min; }
        public int getMax() { return max; }
    }

    public static class AgePointRule {
        private int from;
        private int to;
        private int points;
        public int getFrom() { return from; }
        public int getTo() { return to; }
        public int getPoints() { return points; }
    }

    public static class PointsConfig {
        private Map<String, Integer> virtue;
        private Map<String, Integer> family;
        private Map<String, Integer> weakness;
        private List<AgePointRule> age;

        public Map<String, Integer> getVirtue() { return virtue; }
        public Map<String, Integer> getFamily() { return family; }
        public Map<String, Integer> getWeakness() { return weakness; }
        public List<AgePointRule> getAge() { return age; }
    }

    public static class HouseConfig {
        private String name;
        private PointsConfig points;

        public String getName() { return name; }
        public PointsConfig getPoints() { return points; }
    }

    public static class CouncilRules {
        private String year;
        private Map<String, String> applicationDates;
        private AgeRange ageRange;
        private int places;
        private List<String> bannedFamilies;
        private List<String> unacceptableWeaknesses;
        private List<String> invitations;
        private PointsConfig points;
        private List<HouseConfig> houses;

        public Map<String, String> getApplicationDates() { return applicationDates; }
        public AgeRange getAgeRange() { return ageRange; }
        public int getPlaces() { return places; }
        public List<String> getBannedFamilies() { return bannedFamilies; }
        public List<String> getUnacceptableWeaknesses() { return unacceptableWeaknesses; }
        public List<String> getInvitations() { return invitations; }
        public PointsConfig getPoints() { return points; }
        public List<HouseConfig> houses() { return houses; }
        public List<HouseConfig> getHouses() { return houses; }
    }

    public static class StudentResult {
        private Application application;
        private boolean accepted;
        private String rejectionReason;
        private int score;
        private int rank;
        private String house;

        public StudentResult(Application application) {
            this.application = application;
        }

        public Application getApplication() { return application; }
        public boolean isAccepted() { return accepted; }
        public void setAccepted(boolean accepted) { this.accepted = accepted; }
        public String getRejectionReason() { return rejectionReason; }
        public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getHouse() { return house; }
        public void setHouse(String house) { this.house = house; }
    }
    public static class ApplicationsContainer {
        private List<Application> applications;
        public List<Application> getApplications() { return applications; }
    }
}