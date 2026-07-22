package com.example.doglistener.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidenceSampleStoreTest {

    private ConfidenceSampleStore store;

    @BeforeEach
    void setUp() {
        store = new ConfidenceSampleStore();
    }

    @Test
    void recordsConfidenceSample() {
        Instant recordedAt =
                Instant.parse(
                        "2026-07-22T20:00:00Z"
                );

        store.record(
                0.75f,
                recordedAt
        );

        List<ConfidenceSample> samples =
                store.findSince(
                        recordedAt.minusSeconds(1)
                );

        assertEquals(
                1,
                samples.size()
        );

        assertEquals(
                0.75f,
                samples.get(0).confidence()
        );

        assertEquals(
                recordedAt,
                samples.get(0).recordedAt()
        );
    }

    @Test
    void excludesSamplesBeforeCutoff() {
        store.record(
                0.25f,
                Instant.parse(
                        "2026-07-22T19:00:00Z"
                )
        );

        store.record(
                0.80f,
                Instant.parse(
                        "2026-07-22T20:00:00Z"
                )
        );

        List<ConfidenceSample> samples =
                store.findSince(
                        Instant.parse(
                                "2026-07-22T19:30:00Z"
                        )
                );

        assertEquals(
                1,
                samples.size()
        );

        assertEquals(
                0.80f,
                samples.get(0).confidence()
        );
    }

    @Test
    void acceptsBoundaryConfidenceValues() {
        store.record(
                0.0f,
                Instant.now()
        );

        store.record(
                1.0f,
                Instant.now()
        );

        assertEquals(
                2,
                store.size()
        );
    }

    @Test
    void rejectsInvalidConfidence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> store.record(
                        -0.01f,
                        Instant.now()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> store.record(
                        1.01f,
                        Instant.now()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> store.record(
                        Float.NaN,
                        Instant.now()
                )
        );
    }

    @Test
    void rejectsNullTimestamp() {
        assertThrows(
                IllegalArgumentException.class,
                () -> store.record(
                        0.5f,
                        null
                )
        );
    }
}