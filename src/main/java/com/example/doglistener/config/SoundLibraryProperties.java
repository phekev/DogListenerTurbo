package com.example.doglistener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "response")
public class SoundLibraryProperties {

    private String soundDirectory =
            "./doglistener-data/sounds";

    public String getSoundDirectory() {
        return soundDirectory;
    }

    public void setSoundDirectory(
            String soundDirectory
    ) {
        this.soundDirectory = soundDirectory;
    }
}