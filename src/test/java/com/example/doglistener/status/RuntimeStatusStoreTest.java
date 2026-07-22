package com.example.doglistener.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeStatusStoreTest {

    private RuntimeStatusStore store;

    @BeforeEach
    void setUp() {
        store = new RuntimeStatusStore();
    }

    @Test
    void initialStatusIsStopped() {
        RuntimeStatusSnapshot snapshot =
                store.snapshot();

        assertFalse(
                snapshot.detectionRunning()
        );

        assertFalse(
                snapshot.microphoneRunning()
        );

        assertEquals(
                null,
                snapshot.latestConfidence()
        );
    }

    @Test
    void recordsDetectionAndMicrophoneState() {
        store.markDetectionStarted(
                0.20d
        );

        store.markMicrophoneStarted();

        RuntimeStatusSnapshot running =
                store.snapshot();

        assertTrue(
                running.detectionRunning()
        );

        assertTrue(
                running.microphoneRunning()
        );

        assertEquals(
                0.20d,
                running.confidenceThreshold()
        );

        store.markDetectionStopped();

        RuntimeStatusSnapshot stopped =
                store.snapshot();

        assertFalse(
                stopped.detectionRunning()
        );

        assertFalse(
                stopped.microphoneRunning()
        );
    }

    @Test
    void recordsProcessingTimesAndConfidence() {
        Instant audioTime =
                Instant.parse(
                        "2026-07-22T10:00:00Z"
                );

        Instant predictionTime =
                Instant.parse(
                        "2026-07-22T10:00:01Z"
                );

        Instant barkTime =
                Instant.parse(
                        "2026-07-22T10:00:02Z"
                );

        store.markAudioChunkReceived(
                audioTime
        );

        store.markPrediction(
                0.75f,
                predictionTime
        );

        store.markBarkDetected(
                barkTime
        );

        RuntimeStatusSnapshot snapshot =
                store.snapshot();

        assertEquals(
                audioTime,
                snapshot.lastAudioChunkAt()
        );

        assertEquals(
                predictionTime,
                snapshot.lastPredictionAt()
        );

        assertEquals(
                barkTime,
                snapshot.lastBarkAt()
        );

        assertEquals(
                0.75f,
                snapshot.latestConfidence()
        );
    }

    @Test
    void rejectsInvalidConfidenceValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> store.markDetectionStarted(
                        -0.01d
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> store.markPrediction(
                        1.01f,
                        Instant.now()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> store.markPrediction(
                        Float.NaN,
                        Instant.now()
                )
        );
    }

    @Test
    void rejectsNullTimestamps() {
        assertThrows(
                IllegalArgumentException.class,
                () -> store
                        .markAudioChunkReceived(
                                null
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> store.markPrediction(
                        0.5f,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> store
                        .markBarkDetected(
                                null
                        )
        );
    }
}