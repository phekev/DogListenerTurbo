package com.example.doglistener.web;

import java.time.Instant;

public record SystemStatusResponse(
        String applicationStatus,
        String liveness,
        String readiness,
        boolean detectionRunning,
        boolean microphoneRunning,
        Double confidenceThreshold,
        Float latestConfidence,
        Instant lastAudioChunkAt,
        Instant lastPredictionAt,
        Long latestPredictionAgeMillis,
        Instant lastBarkAt,
        long uptimeMillis,
        Instant serverTime
) {
}