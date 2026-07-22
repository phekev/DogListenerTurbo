package com.example.doglistener.status;

import java.time.Instant;

public record RuntimeStatusSnapshot(
        boolean detectionRunning,
        boolean microphoneRunning,
        Double confidenceThreshold,
        Float latestConfidence,
        Instant lastAudioChunkAt,
        Instant lastPredictionAt,
        Instant lastBarkAt
) {
}