
package com.example.doglistener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "response-history")
public class ResponseHistoryProperties {

    private String databaseFile =
            "/home/kevin/doglistener-data/logs/"
                    + "response-history.db";

    public String getDatabaseFile() {
        return databaseFile;
    }

    public void setDatabaseFile(
            String databaseFile
    ) {
        this.databaseFile = databaseFile;
    }
}