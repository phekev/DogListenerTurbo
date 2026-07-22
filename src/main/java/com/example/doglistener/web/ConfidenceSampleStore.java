package com.example.doglistener.web;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class ConfidenceSampleStore {

    private static final int MAXIMUM_SAMPLES =
            3_600;

    private final Deque<ConfidenceSample> samples =
            new ArrayDeque<>();

    public synchronized void record(
            float confidence
    ) {
        record(
                confidence,
                Instant.now()
        );
    }

    public synchronized void record(
            float confidence,
            Instant recordedAt
    ) {
        validateConfidence(confidence);

        if (recordedAt == null) {
            throw new IllegalArgumentException(
                    "Recorded-at time must not be null."
            );
        }

        samples.addLast(
                new ConfidenceSample(
                        recordedAt,
                        confidence
                )
        );

        while (samples.size()
                > MAXIMUM_SAMPLES) {

            samples.removeFirst();
        }
    }

    public synchronized List<ConfidenceSample> findSince(
            Instant cutoff
    ) {
        if (cutoff == null) {
            throw new IllegalArgumentException(
                    "Confidence cutoff must not be null."
            );
        }

        List<ConfidenceSample> results =
                new ArrayList<>();

        for (ConfidenceSample sample : samples) {
            if (!sample.recordedAt()
                    .isBefore(cutoff)) {

                results.add(sample);
            }
        }

        return List.copyOf(results);
    }

    public synchronized int size() {
        return samples.size();
    }

    private void validateConfidence(
            float confidence
    ) {
        if (!Float.isFinite(confidence)
                || confidence < 0.0f
                || confidence > 1.0f) {

            throw new IllegalArgumentException(
                    "Confidence must be between "
                            + "0.0 and 1.0."
            );
        }
    }
}