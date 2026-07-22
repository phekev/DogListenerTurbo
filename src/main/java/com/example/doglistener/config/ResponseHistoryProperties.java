package com.example.doglistener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "response-history")
public class ResponseHistoryProperties {

    private String databaseFile =
            "/home/kevin/doglistener-data/logs/"
                    + "response-history.db";

    private boolean reportEnabled = true;

    private int recentReportLimit = 20;

    private String reportTimeZone =
            "Europe/Dublin";

    public String getDatabaseFile() {
        return databaseFile;
    }

    public void setDatabaseFile(
            String databaseFile
    ) {
        this.databaseFile = databaseFile;
    }

    public boolean isReportEnabled() {
        return reportEnabled;
    }

    public void setReportEnabled(
            boolean reportEnabled
    ) {
        this.reportEnabled = reportEnabled;
    }

    public int getRecentReportLimit() {
        return recentReportLimit;
    }

    public void setRecentReportLimit(
            int recentReportLimit
    ) {
        this.recentReportLimit =
                recentReportLimit;
    }

    public String getReportTimeZone() {
        return reportTimeZone;
    }

    public void setReportTimeZone(
            String reportTimeZone
    ) {
        this.reportTimeZone =
                reportTimeZone;
    }
}