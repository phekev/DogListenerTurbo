package com.example.doglistener.history;

import com.example.doglistener.audio.ResponseLevel;

import java.nio.file.Path;
import java.time.Instant;

public record ResponseHistoryEntry(
        long id,
        Instant playedAt,
        ResponseLevel responseLevel,
        Path soundFile
) {
}