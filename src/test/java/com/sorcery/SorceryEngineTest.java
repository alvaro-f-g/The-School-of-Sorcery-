package com.sorcery;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SorceryEngineTest {

    @Test
    void shouldProcessAllApplicationsAndAcceptExactly50() throws Exception {

        SorceryEngine engine = new SorceryEngine();
        engine.loadData(Paths.get("data"));

        List<Model.StudentResult> results = engine.process();

        assertEquals(200, results.size());

        long accepted = results.stream()
                .filter(Model.StudentResult::isAccepted)
                .count();

        long rejected = results.stream()
                .filter(result -> !result.isAccepted())
                .count();

        assertEquals(50, accepted);
        assertEquals(150, rejected);
    }


    @Test
    void invitedApplicantShouldAlwaysBeAccepted() throws Exception {

        SorceryEngine engine = new SorceryEngine();
        engine.loadData(Paths.get("data"));

        List<Model.StudentResult> results = engine.process();

        Model.StudentResult tobias = results.stream()
                .filter(result ->
                        result.getApplication()
                                .getId()
                                .equals("A031")
                )
                .findFirst()
                .orElseThrow();

        assertTrue(
                tobias.isAccepted(),
                "An invited applicant must always be accepted"
        );

        assertNotNull(
                tobias.getHouse(),
                "An accepted invited applicant must have a house"
        );
    }


    @Test
    void vetoedApplicantsShouldNotReceiveRankingPosition() throws Exception {

        SorceryEngine engine = new SorceryEngine();
        engine.loadData(Paths.get("data"));

        List<Model.StudentResult> results = engine.process();

        List<Model.StudentResult> vetoed = results.stream()
                .filter(result ->
                        !result.isAccepted()
                        && result.getRejectionReason() != null
                        && !result.getRejectionReason().matches("\\d+")
                )
                .toList();

        assertFalse(
                vetoed.isEmpty(),
                "There should be vetoed applications in the dataset"
        );

        for (Model.StudentResult result : vetoed) {

            assertEquals(
                    0,
                    result.getRank(),
                    "Vetoed applicants must not have a ranking position"
            );
        }
    }


    @Test
    void applicantsRejectedForNoPlacesShouldKeepTheirRank() throws Exception {

        SorceryEngine engine = new SorceryEngine();
        engine.loadData(Paths.get("data"));

        List<Model.StudentResult> results = engine.process();

        List<Model.StudentResult> rejectedByPosition = results.stream()
                .filter(result ->
                        !result.isAccepted()
                        && result.getRejectionReason() != null
                        && result.getRejectionReason().matches("\\d+")
                )
                .toList();

        assertFalse(
                rejectedByPosition.isEmpty(),
                "There should be candidates rejected because no places remain"
        );

        for (Model.StudentResult result : rejectedByPosition) {

            assertEquals(
                    String.valueOf(result.getRank()),
                    result.getRejectionReason(),
                    "The rejection reason must be the applicant's ranking position"
            );

            assertTrue(
                    result.getRank() > 0,
                    "Applicants rejected for no places must keep their rank"
            );
        }
    }


    @Test
    void acceptedApplicantsShouldHaveAHouse() throws Exception {

        SorceryEngine engine = new SorceryEngine();
        engine.loadData(Paths.get("data"));

        List<Model.StudentResult> results = engine.process();

        for (Model.StudentResult result : results) {

            if (result.isAccepted()) {

                assertNotNull(
                        result.getHouse(),
                        "Every accepted applicant must have a house"
                );

                assertFalse(
                        result.getHouse().isBlank(),
                        "House name cannot be blank"
                );
            }
        }
    }


        @Test
        void processingShouldBeDeterministicRegardlessOfInputOrder() throws Exception {

        Gson gson = new Gson();

        Model.ApplicationsContainer container =
        gson.fromJson(
                new FileReader("data/applications.json"),
                Model.ApplicationsContainer.class
        );

        List<Model.Application> originalApplications =
        new ArrayList<>(container.getApplications());

        List<Model.Application> shuffledApplications =
        new ArrayList<>(originalApplications);

        Collections.shuffle(
        shuffledApplications,
        new Random(12345)
        );


        SorceryEngine originalEngine =
        new SorceryEngine();

        originalEngine.loadRules(
        Paths.get("data")
        );

        originalEngine.setApplications(
        originalApplications
        );

        List<Model.StudentResult> originalResults =
        originalEngine.process();


        SorceryEngine shuffledEngine =
        new SorceryEngine();

        shuffledEngine.loadRules(
        Paths.get("data")
        );

        shuffledEngine.setApplications(
        shuffledApplications
        );

        List<Model.StudentResult> shuffledResults =
        shuffledEngine.process();


        assertEquals(
        normalizeResults(originalResults),
        normalizeResults(shuffledResults),
        "Shuffling the input applications must not change the final result"
        );
        }
        @Test
        void vetoesShouldBeReportedInTheSpecifiedOrder() throws Exception {

        Gson gson = new Gson();

        String applicationsJson = """
                {
                "applications": [
                {
                "id": "TEST-VETO",
                "firstName": "Multi",
                "familyName": "Blackcrow",
                "age": 99,
                "virtue": "cunning",
                "weakness": "cruelty",
                "applicationDate": "2099-01-01"
                }
                ]
                }
                """;

        Model.ApplicationsContainer container =
                gson.fromJson(
                        applicationsJson,
                        Model.ApplicationsContainer.class
                );

        SorceryEngine engine = new SorceryEngine();

        engine.loadRules(Paths.get("data"));
        engine.setApplications(container.getApplications());

        List<Model.StudentResult> results =
                engine.process();

        assertEquals(1, results.size());

        Model.StudentResult result =
                results.get(0);

        
        assertEquals(
                "Banned family",
                result.getRejectionReason(),
                "Banned family must be reported before age, weakness or date vetoes"
        );
        }

        @Test
        void rankingShouldUseAgeFamilyNameAndFirstNameAsTieBreakers() throws Exception {

        Gson gson = new Gson();

        String applicationsJson = """
                {
                "applications": [
                {
                "id": "TIE-4",
                "firstName": "Zoe",
                "familyName": "Brown",
                "age": 13,
                "virtue": "unknown",
                "weakness": "unknown",
                "applicationDate": "2026-03-10"
                },
                {
                "id": "TIE-3",
                "firstName": "Adam",
                "familyName": "Brown",
                "age": 12,
                "virtue": "unknown",
                "weakness": "unknown",
                "applicationDate": "2026-03-10"
                },
                {
                "id": "TIE-2",
                "firstName": "Zoe",
                "familyName": "Adams",
                "age": 12,
                "virtue": "unknown",
                "weakness": "unknown",
                "applicationDate": "2026-03-10"
                },
                {
                "id": "TIE-1",
                "firstName": "Aaron",
                "familyName": "Adams",
                "age": 12,
                "virtue": "unknown",
                "weakness": "unknown",
                "applicationDate": "2026-03-10"
                }
                ]
                }
                """;

        Model.ApplicationsContainer container =
                gson.fromJson(
                        applicationsJson,
                        Model.ApplicationsContainer.class
                );

        SorceryEngine engine = new SorceryEngine();

        engine.loadRules(Paths.get("data"));
        engine.setApplications(container.getApplications());

        List<Model.StudentResult> results =
                engine.process();

        assertEquals(4, results.size());

        /*
        * "unknown" esta intencionalmente no presente en el archivo de puntos,
        * por lo que cada solicitante recibe una puntuación de 0.
        *
        * Orden esperado:
        *
        * 1. Aaron Adams, 12
        * 2. Zoe Adams, 12
        * 3. Adam Brown, 12
        * 4. Zoe Brown, 13
        */

        assertEquals(
                "TIE-1",
                results.get(0).getApplication().getId()
        );

        assertEquals(
                "TIE-2",
                results.get(1).getApplication().getId()
        );

        assertEquals(
                "TIE-3",
                results.get(2).getApplication().getId()
        );

        assertEquals(
                "TIE-4",
                results.get(3).getApplication().getId()
        );

        assertEquals(1, results.get(0).getRank());
        assertEquals(2, results.get(1).getRank());
        assertEquals(3, results.get(2).getRank());
        assertEquals(4, results.get(3).getRank());
        }
    private List<String> normalizeResults(
            List<Model.StudentResult> results) {

        return results.stream()
                .map(result ->

                        result.getApplication().getId()
                        + "|"
                        + result.isAccepted()
                        + "|"
                        + result.getScore()
                        + "|"
                        + result.getRank()
                        + "|"
                        + String.valueOf(result.getHouse())
                        + "|"
                        + String.valueOf(result.getRejectionReason())
                )
                .collect(Collectors.toList());
    }
}