package com.example.doglistener.status;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RuntimeStatusStore {

    private boolean detectionRunning;
    private boolean microphoneRunning;

    private Double confidenceThreshold;
    private Float latestConfidence;

    private Instant lastAudioChunkAt;
    private Instant lastPredictionAt;
    private Instant lastBarkAt;

    public synchronized void markDetectionStarted(
            double confidenceThreshold
    ) {
        validateConfidence(
                confidenceThreshold,
                "Confidence threshold"
        );

        this.confidenceThreshold =
                confidenceThreshold;

        detectionRunning = true;
    }

    public synchronized void markDetectionStopped() {
        detectionRunning = false;
        microphoneRunning = false;
    }

    public synchronized void markMicrophoneStarted() {
        microphoneRunning = true;
    }

    public synchronized void markMicrophoneStopped() {
        microphoneRunning = false;
    }

    public synchronized void markAudioChunkReceived(
            Instant receivedAt
    ) {
        lastAudioChunkAt =
                requireTimestamp(
                        receivedAt,
                        "Audio chunk time"
                );
    }

    public synchronized void markPrediction(
            float confidence,
            Instant predictedAt
    ) {
        validateConfidence(
                confidence,
                "Prediction confidence"
        );

        latestConfidence = confidence;

        lastPredictionAt =
                requireTimestamp(
                        predictedAt,
                        "Prediction time"
                );
    }

    public synchronized void markBarkDetected(
            Instant detectedAt
    ) {
        lastBarkAt =
                requireTimestamp(
                        detectedAt,
                        "Bark detection time"
                );
    }

    public synchronized RuntimeStatusSnapshot snapshot() {
        return new RuntimeStatusSnapshot(
                detectionRunning,
                microphoneRunning,
                confidenceThreshold,
                latestConfidence,
                lastAudioChunkAt,
                lastPredictionAt,
                lastBarkAt
        );
    }

    private void validateConfidence(
            double confidence,
            String description
    ) {
        if (!Double.isFinite(confidence)
                || confidence < 0.0d
                || confidence > 1.0d) {

            throw new IllegalArgumentException(
                    description
                            + " must be between "
                            + "0.0 and 1.0."
            );
        }
    }

    private Instant requireTimestamp(
            Instant timestamp,
            String description
    ) {
        if (timestamp == null) {
            throw new IllegalArgumentException(
                    description
                            + " must not be null."
            );
        }

        return timestamp;
    }
}