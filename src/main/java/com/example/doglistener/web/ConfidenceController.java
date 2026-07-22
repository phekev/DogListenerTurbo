package com.example.doglistener.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/confidence")
public class ConfidenceController {

    private static final int MAXIMUM_MINUTES =
            1_440;

    private final ConfidenceSampleStore sampleStore;

    public ConfidenceController(
            ConfidenceSampleStore sampleStore
    ) {
        this.sampleStore = sampleStore;
    }

    @GetMapping
    public List<ConfidenceSample> getConfidence(
            @RequestParam(
                    defaultValue = "10"
            )
            int minutes
    ) {
        if (minutes <= 0
                || minutes > MAXIMUM_MINUTES) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minutes must be between 1 and "
                            + MAXIMUM_MINUTES + "."
            );
        }

        Instant cutoff =
                Instant.now().minus(
                        Duration.ofMinutes(minutes)
                );

        return sampleStore.findSince(cutoff);
    }
}