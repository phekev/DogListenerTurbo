package com.example.doglistener.web;

import com.example.doglistener.status.RuntimeStatusSnapshot;
import com.example.doglistener.status.RuntimeStatusStore;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/status")
public class SystemStatusController {

    private final ApplicationAvailability availability;
    private final RuntimeStatusStore runtimeStatusStore;

    public SystemStatusController(
            ApplicationAvailability availability,
            RuntimeStatusStore runtimeStatusStore
    ) {
        this.availability = availability;
        this.runtimeStatusStore =
                runtimeStatusStore;
    }

    @GetMapping
    public SystemStatusResponse getStatus() {
        Instant serverTime =
                Instant.now();

        RuntimeStatusSnapshot runtimeStatus =
                runtimeStatusStore.snapshot();

        Long latestPredictionAgeMillis =
                calculatePredictionAge(
                        runtimeStatus
                                .lastPredictionAt(),
                        serverTime
                );

        return new SystemStatusResponse(
                "RUNNING",
                availability
                        .getLivenessState()
                        .toString(),
                availability
                        .getReadinessState()
                        .toString(),
                runtimeStatus
                        .detectionRunning(),
                runtimeStatus
                        .microphoneRunning(),
                runtimeStatus
                        .confidenceThreshold(),
                runtimeStatus
                        .latestConfidence(),
                runtimeStatus
                        .lastAudioChunkAt(),
                runtimeStatus
                        .lastPredictionAt(),
                latestPredictionAgeMillis,
                runtimeStatus
                        .lastBarkAt(),
                ManagementFactory
                        .getRuntimeMXBean()
                        .getUptime(),
                serverTime
        );
    }

    private Long calculatePredictionAge(
            Instant lastPredictionAt,
            Instant serverTime
    ) {
        if (lastPredictionAt == null) {
            return null;
        }

        long age =
                Duration.between(
                        lastPredictionAt,
                        serverTime
                ).toMillis();

        return Math.max(
                0L,
                age
        );
    }
}