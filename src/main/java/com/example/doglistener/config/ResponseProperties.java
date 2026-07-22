package com.example.doglistener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "response")
public class ResponseProperties {

    private float volume = 1.0f;

    private long secondResponseDelayMillis = 10_000;
    private long prolongedResponseDelayMillis = 60_000;
    private long quietResetMillis = 10_000;

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public long getSecondResponseDelayMillis() {
        return secondResponseDelayMillis;
    }

    public void setSecondResponseDelayMillis(
            long secondResponseDelayMillis
    ) {
        this.secondResponseDelayMillis =
                secondResponseDelayMillis;
    }

    public long getProlongedResponseDelayMillis() {
        return prolongedResponseDelayMillis;
    }

    public void setProlongedResponseDelayMillis(
            long prolongedResponseDelayMillis
    ) {
        this.prolongedResponseDelayMillis =
                prolongedResponseDelayMillis;
    }

    public long getQuietResetMillis() {
        return quietResetMillis;
    }

    public void setQuietResetMillis(
            long quietResetMillis
    ) {
        this.quietResetMillis = quietResetMillis;
    }
}