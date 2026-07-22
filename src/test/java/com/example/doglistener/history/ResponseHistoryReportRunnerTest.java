package com.example.doglistener.history;

import com.example.doglistener.config.ResponseHistoryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseHistoryReportRunnerTest {

    @Mock
    private ResponseHistoryReportService reportService;

    private ResponseHistoryProperties properties;
    private ResponseHistoryReportRunner runner;

    @BeforeEach
    void setUp() {
        properties =
                new ResponseHistoryProperties();

        runner =
                new ResponseHistoryReportRunner(
                        properties,
                        reportService
                );
    }

    @Test
    void generatesReportWhenEnabled() {
        properties.setReportEnabled(true);

        when(
                reportService.buildReport()
        ).thenReturn(
                "Test response report"
        );

        runner.run(null);

        verify(reportService).buildReport();
    }

    @Test
    void doesNotGenerateReportWhenDisabled() {
        properties.setReportEnabled(false);

        runner.run(null);

        verify(
                reportService,
                never()
        ).buildReport();
    }

    @Test
    void reportFailureDoesNotEscapeRunner() {
        properties.setReportEnabled(true);

        when(
                reportService.buildReport()
        ).thenThrow(
                new IllegalStateException(
                        "Simulated reporting failure"
                )
        );

        runner.run(null);

        verify(reportService).buildReport();
    }
}