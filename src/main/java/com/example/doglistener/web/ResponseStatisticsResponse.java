package com.example.doglistener.web;

public record ResponseStatisticsResponse(
        long firstResponses,
        long secondResponses,
        long prolongedResponses,
        long overallResponses
) {
}