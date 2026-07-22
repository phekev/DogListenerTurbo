package com.example.doglistener.web;

import java.time.Instant;

public record ResponseHistoryItemResponse(
        long id,
        Instant playedAt,
        String responseLevel,
        String filename,
        String soundFile
) {
}