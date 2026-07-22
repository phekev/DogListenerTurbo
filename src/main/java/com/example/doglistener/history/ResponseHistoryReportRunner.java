package com.example.doglistener.history;

import com.example.doglistener.config.ResponseHistoryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResponseHistoryReportRunner
        implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ResponseHistoryReportRunner.class
            );

    private final ResponseHistoryProperties properties;
    private final ResponseHistoryReportService reportService;

    public ResponseHistoryReportRunner(
            ResponseHistoryProperties properties,
            ResponseHistoryReportService reportService
    ) {
        this.properties = properties;
        this.reportService = reportService;
    }

    @Override
    public void run(
            ApplicationArguments arguments
    ) {
        if (!properties.isReportEnabled()) {
            LOGGER.info(
                    "Startup response history report "
                            + "is disabled."
            );

            return;
        }

        try {
            String report =
                    reportService.buildReport();

            LOGGER.info(
                    "{}{}",
                    System.lineSeparator(),
                    report
            );

        } catch (RuntimeException exception) {
            /*
             * A reporting failure must not prevent
             * bark detection from starting.
             */
            LOGGER.error(
                    "Unable to generate startup "
                            + "response history report.",
                    exception
            );
        }
    }
}