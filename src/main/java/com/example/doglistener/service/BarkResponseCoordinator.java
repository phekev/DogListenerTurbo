package com.example.doglistener.service;

import com.example.doglistener.config.ResponseProperties;
import com.example.doglistener.ml.Prediction;
import com.example.doglistener.sound.ResponseSoundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BarkResponseCoordinator {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BarkResponseCoordinator.class);

    private final ResponseProperties responseProperties;
    private final ResponseSoundPlayer responseSoundPlayer;

    private long barkingSessionStartMillis;
    private long lastBarkTimeMillis;

    private boolean firstResponsePlayed;
    private boolean secondResponsePlayed;
    private boolean prolongedResponsePlayed;

    public BarkResponseCoordinator(
            ResponseProperties responseProperties,
            ResponseSoundPlayer responseSoundPlayer
    ) {
        this.responseProperties = responseProperties;
        this.responseSoundPlayer = responseSoundPlayer;
    }

    public void onBarkDetected(
            Prediction prediction,
            long now
    ) {
        startSessionIfNecessary(now);

        lastBarkTimeMillis = now;

        long sessionDuration =
                now - barkingSessionStartMillis;

        LOGGER.info(
                "Dog bark detected: confidence={}, "
                        + "sessionDuration={} ms",
                prediction.confidence(),
                sessionDuration
        );

        if (!firstResponsePlayed) {
            playFirstResponse();
            return;
        }

        if (!prolongedResponsePlayed
                && sessionDuration
                >= responseProperties
                .getProlongedResponseDelayMillis()) {

            playProlongedResponse();
            return;
        }

        if (!secondResponsePlayed
                && sessionDuration
                >= responseProperties
                .getSecondResponseDelayMillis()) {

            playSecondResponse();
        }
    }

    public void onNoBarkDetected(long now) {
        if (!isSessionActive()) {
            return;
        }

        long quietDuration =
                now - lastBarkTimeMillis;

        if (quietDuration
                < responseProperties.getQuietResetMillis()) {
            return;
        }

        LOGGER.info(
                "Barking session ended after {} ms of quiet.",
                quietDuration
        );

        reset();
    }

    public void reset() {
        barkingSessionStartMillis = 0L;
        lastBarkTimeMillis = 0L;

        firstResponsePlayed = false;
        secondResponsePlayed = false;
        prolongedResponsePlayed = false;
    }

    private void startSessionIfNecessary(long now) {
        if (isSessionActive()) {
            return;
        }

        barkingSessionStartMillis = now;
        lastBarkTimeMillis = now;

        firstResponsePlayed = false;
        secondResponsePlayed = false;
        prolongedResponsePlayed = false;

        LOGGER.info("New barking session started.");
    }

    private boolean isSessionActive() {
        return barkingSessionStartMillis > 0L;
    }

    private void playFirstResponse() {
        firstResponsePlayed = true;

        playSound(
                "first bark",
                responseProperties.getFirstSoundFile()
        );
    }

    private void playSecondResponse() {
        secondResponsePlayed = true;

        playSound(
                "second bark",
                responseProperties.getSecondSoundFile()
        );
    }

    private void playProlongedResponse() {
        prolongedResponsePlayed = true;

        playSound(
                "prolonged-barking",
                responseProperties.getProlongedSoundFile()
        );
    }

    private void playSound(
            String responseName,
            String soundFile
    ) {
        LOGGER.info(
                "Playing {} response: {}",
                responseName,
                soundFile
        );

        responseSoundPlayer.play(soundFile);
    }
}