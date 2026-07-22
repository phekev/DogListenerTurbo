package com.example.doglistener.history;

import com.example.doglistener.config.ResponseHistoryProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ResponseHistoryReportService {

    private static final String LINE_SEPARATOR =
            System.lineSeparator();

    private final ResponseHistoryRepository repository;
    private final ResponseHistoryProperties properties;

    public ResponseHistoryReportService(
            ResponseHistoryRepository repository,
            ResponseHistoryProperties properties
    ) {
        this.repository = repository;
        this.properties = properties;
    }

    public String buildReport() {
        int recentLimit =
                validateRecentLimit(
                        properties.getRecentReportLimit()
                );

        ZoneId reportTimeZone =
                parseTimeZone(
                        properties.getReportTimeZone()
                );

        ResponseStatistics statistics =
                repository.getStatistics();

        List<ResponseHistoryEntry> recentEntries =
                repository.findRecent(
                        recentLimit
                );

        return formatReport(
                statistics,
                recentEntries,
                recentLimit,
                reportTimeZone
        );
    }

    private String formatReport(
            ResponseStatistics statistics,
            List<ResponseHistoryEntry> entries,
            int recentLimit,
            ZoneId reportTimeZone
    ) {
        StringBuilder report =
                new StringBuilder();

        report.append(
                "====================================="
        );

        report.append(LINE_SEPARATOR);
        report.append(" Response History Report");
        report.append(LINE_SEPARATOR);

        report.append(
                "====================================="
        );

        report.append(LINE_SEPARATOR);

        report.append(
                "First responses     : "
        );

        report.append(
                statistics.firstResponses()
        );

        report.append(LINE_SEPARATOR);

        report.append(
                "Second responses    : "
        );

        report.append(
                statistics.secondResponses()
        );

        report.append(LINE_SEPARATOR);

        report.append(
                "Prolonged responses : "
        );

        report.append(
                statistics.prolongedResponses()
        );

        report.append(LINE_SEPARATOR);

        report.append(
                "Overall responses   : "
        );

        report.append(
                statistics.overallResponses()
        );

        report.append(LINE_SEPARATOR);
        report.append(LINE_SEPARATOR);

        report.append(
                "Recent responses (maximum "
        );

        report.append(recentLimit);
        report.append("):");
        report.append(LINE_SEPARATOR);

        if (entries.isEmpty()) {
            report.append(
                    "No responses have been recorded."
            );

            return report.toString();
        }

        DateTimeFormatter formatter =
                DateTimeFormatter
                        .ofPattern(
                                "yyyy-MM-dd HH:mm:ss z"
                        )
                        .withZone(reportTimeZone);

        for (ResponseHistoryEntry entry : entries) {
            appendEntry(
                    report,
                    entry,
                    formatter
            );
        }

        return report.toString();
    }

    private void appendEntry(
            StringBuilder report,
            ResponseHistoryEntry entry,
            DateTimeFormatter formatter
    ) {
        Path soundFile =
                entry.soundFile();

        String filename =
                soundFile.getFileName() == null
                        ? soundFile.toString()
                        : soundFile
                        .getFileName()
                        .toString();

        report.append("- ");

        report.append(
                formatter.format(
                        entry.playedAt()
                )
        );

        report.append(" | ");

        report.append(
                entry.responseLevel()
        );

        report.append(" | ");

        report.append(filename);

        report.append(LINE_SEPARATOR);

        report.append("  ");

        report.append(
                soundFile
                        .toAbsolutePath()
                        .normalize()
        );

        report.append(LINE_SEPARATOR);
    }

    private int validateRecentLimit(
            int recentLimit
    ) {
        if (recentLimit <= 0) {
            throw new IllegalStateException(
                    "Response history recent report "
                            + "limit must be greater than zero."
            );
        }

        return recentLimit;
    }

    private ZoneId parseTimeZone(
            String configuredTimeZone
    ) {
        if (configuredTimeZone == null
                || configuredTimeZone.isBlank()) {

            throw new IllegalStateException(
                    "Response history report time zone "
                            + "must not be blank."
            );
        }

        try {
            return ZoneId.of(
                    configuredTimeZone.trim()
            );

        } catch (DateTimeException exception) {
            throw new IllegalStateException(
                    "Invalid response history report "
                            + "time zone: "
                            + configuredTimeZone,
                    exception
            );
        }
    }
}