package com.sorcery;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SorceryEngineTest {

    @Test
    void shouldProcessAllApplicationsAndAcceptExactly50() throws Exception {

        SorceryEngine engine = new SorceryEngine();

        engine.loadData(Paths.get("data"));

        List<Model.StudentResult> results =
                engine.process();

        assertEquals(
                200,
                results.size(),
                "All 200 applications must be processed"
        );

        long accepted =
                results.stream()
                        .filter(Model.StudentResult::isAccepted)
                        .count();

        long rejected =
                results.stream()
                        .filter(result -> !result.isAccepted())
                        .count();

        assertEquals(
                50,
                accepted,
                "Exactly 50 applicants must be accepted"
        );

        assertEquals(
                150,
                rejected,
                "Exactly 150 applicants must be rejected"
        );
    }
}