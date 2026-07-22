package com.example.doglistener.web;

import java.time.Instant;

public record ConfidenceSample(
        Instant recordedAt,
        float confidence
) {
}