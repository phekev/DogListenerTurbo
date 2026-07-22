package com.example.doglistener.history;

import com.example.doglistener.audio.ResponseLevel;
import com.example.doglistener.config.ResponseHistoryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseHistoryReportServiceTest {

    @Mock
    private ResponseHistoryRepository repository;

    private ResponseHistoryProperties properties;
    private ResponseHistoryReportService service;

    @BeforeEach
    void setUp() {
        properties =
                new ResponseHistoryProperties();

        properties.setRecentReportLimit(20);
        properties.setReportTimeZone(
                "Europe/Dublin"
        );

        service =
                new ResponseHistoryReportService(
                        repository,
                        properties
                );
    }

    @Test
    void includesPersistentTotalsInReport() {
        when(
                repository.getStatistics()
        ).thenReturn(
                new ResponseStatistics(
                        5L,
                        3L,
                        1L,
                        9L
                )
        );

        when(
                repository.findRecent(20)
        ).thenReturn(List.of());

        String report =
                service.buildReport();

        assertTrue(
                report.contains(
                        "First responses     : 5"
                )
        );

        assertTrue(
                report.contains(
                        "Second responses    : 3"
                )
        );

        assertTrue(
                report.contains(
                        "Prolonged responses : 1"
                )
        );

        assertTrue(
                report.contains(
                        "Overall responses   : 9"
                )
        );
    }

    @Test
    void includesRecentResponseDetails() {
        Path soundFile =
                Path.of(
                        "/home/kevin/"
                                + "doglistener-data/sounds/"
                                + "response_one/hey.wav"
                );

        ResponseHistoryEntry entry =
                new ResponseHistoryEntry(
                        1L,
                        Instant.parse(
                                "2026-07-22T18:30:12Z"
                        ),
                        ResponseLevel.FIRST,
                        soundFile
                );

        when(
                repository.getStatistics()
        ).thenReturn(
                new ResponseStatistics(
                        1L,
                        0L,
                        0L,
                        1L
                )
        );

        when(
                repository.findRecent(20)
        ).thenReturn(
                List.of(entry)
        );

        String report =
                service.buildReport();

        /*
         * 18:30 UTC is 19:30 in Dublin during
         * Irish Summer Time.
         */
        assertTrue(
                report.contains(
                        "2026-07-22 19:30:12 IST"
                )
        );

        assertTrue(
                report.contains("FIRST")
        );

        assertTrue(
                report.contains("hey.wav")
        );

        assertTrue(
                report.contains(
                        soundFile.toString()
                )
        );
    }

    @Test
    void reportsWhenHistoryIsEmpty() {
        when(
                repository.getStatistics()
        ).thenReturn(
                new ResponseStatistics(
                        0L,
                        0L,
                        0L,
                        0L
                )
        );

        when(
                repository.findRecent(20)
        ).thenReturn(List.of());

        String report =
                service.buildReport();

        assertTrue(
                report.contains(
                        "No responses have been recorded."
                )
        );
    }

    @Test
    void usesConfiguredRecentLimit() {
        properties.setRecentReportLimit(7);

        when(
                repository.getStatistics()
        ).thenReturn(
                new ResponseStatistics(
                        0L,
                        0L,
                        0L,
                        0L
                )
        );

        when(
                repository.findRecent(7)
        ).thenReturn(List.of());

        service.buildReport();

        verify(repository).findRecent(7);
    }

    @Test
    void rejectsInvalidRecentLimit() {
        properties.setRecentReportLimit(0);

        assertThrows(
                IllegalStateException.class,
                service::buildReport
        );
    }

    @Test
    void rejectsInvalidTimeZone() {
        properties.setReportTimeZone(
                "Not/A-Time-Zone"
        );

        assertThrows(
                IllegalStateException.class,
                service::buildReport
        );
    }
}