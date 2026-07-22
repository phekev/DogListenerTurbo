package com.example.doglistener.web;

import java.time.Instant;
import java.util.List;

public record ResponseTimelineResponse(
        Instant windowStart,
        Instant windowEnd,
        List<ResponseHistoryItemResponse> responses
) {
}