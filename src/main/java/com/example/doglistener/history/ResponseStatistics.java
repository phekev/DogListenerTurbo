package com.example.doglistener.history;

import com.example.doglistener.audio.ResponseLevel;

public record ResponseStatistics(
        long firstResponses,
        long secondResponses,
        long prolongedResponses,
        long overallResponses
) {

    public long countFor(
            ResponseLevel responseLevel
    ) {
        if (responseLevel == null) {
            throw new IllegalArgumentException(
                    "Response level must not be null."
            );
        }

        return switch (responseLevel) {
            case FIRST -> firstResponses;
            case SECOND -> secondResponses;
            case PROLONGED -> prolongedResponses;
        };
    }
}