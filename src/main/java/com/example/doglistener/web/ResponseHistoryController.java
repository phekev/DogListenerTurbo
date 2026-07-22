package com.example.doglistener.web;

import com.example.doglistener.history.ResponseHistoryEntry;
import com.example.doglistener.history.ResponseHistoryRepository;
import com.example.doglistener.history.ResponseStatistics;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/responses")
public class ResponseHistoryController {

    private static final int MAXIMUM_RESULTS =
            500;

    private static final int MAXIMUM_TIMELINE_HOURS =
            168;

    private final ResponseHistoryRepository repository;

    public ResponseHistoryController(
            ResponseHistoryRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping
    public List<ResponseHistoryItemResponse> getRecent(
            @RequestParam(
                    defaultValue = "50"
            )
            int limit
    ) {
        validateLimit(limit);

        return repository
                .findRecent(limit)
                .stream()
                .map(this::mapEntry)
                .toList();
    }

    @GetMapping("/statistics")
    public ResponseStatisticsResponse getStatistics() {
        ResponseStatistics statistics =
                repository.getStatistics();

        return new ResponseStatisticsResponse(
                statistics.firstResponses(),
                statistics.secondResponses(),
                statistics.prolongedResponses(),
                statistics.overallResponses()
        );
    }

    @GetMapping("/timeline")
    public ResponseTimelineResponse getTimeline(
            @RequestParam(
                    defaultValue = "24"
            )
            int hours
    ) {
        validateTimelineHours(hours);

        Instant windowEnd =
                Instant.now();

        Instant windowStart =
                windowEnd.minus(
                        Duration.ofHours(hours)
                );

        List<ResponseHistoryItemResponse> responses =
                repository
                        .findBetween(
                                windowStart,
                                windowEnd
                        )
                        .stream()
                        .map(this::mapEntry)
                        .toList();

        return new ResponseTimelineResponse(
                windowStart,
                windowEnd,
                responses
        );
    }

    private ResponseHistoryItemResponse mapEntry(
            ResponseHistoryEntry entry
    ) {
        Path soundFile =
                entry.soundFile()
                        .toAbsolutePath()
                        .normalize();

        Path filename =
                soundFile.getFileName();

        return new ResponseHistoryItemResponse(
                entry.id(),
                entry.playedAt(),
                entry.responseLevel().name(),
                filename == null
                        ? soundFile.toString()
                        : filename.toString(),
                soundFile.toString()
        );
    }

    private void validateLimit(int limit) {
        if (limit <= 0
                || limit > MAXIMUM_RESULTS) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Limit must be between 1 and "
                            + MAXIMUM_RESULTS + "."
            );
        }
    }

    private void validateTimelineHours(int hours) {
        if (hours <= 0
                || hours > MAXIMUM_TIMELINE_HOURS) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Timeline hours must be between 1 and "
                            + MAXIMUM_TIMELINE_HOURS + "."
            );
        }
    }
}