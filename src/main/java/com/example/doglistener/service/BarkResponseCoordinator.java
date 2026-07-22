package com.example.doglistener.service;

import com.example.doglistener.audio.RandomResponseSelector;
import com.example.doglistener.audio.ResponseLevel;
import com.example.doglistener.config.ResponseProperties;
import com.example.doglistener.history.ResponseHistoryRepository;
import com.example.doglistener.history.ResponseStatistics;
import com.example.doglistener.ml.Prediction;
import com.example.doglistener.sound.ResponseSoundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@Component
public class BarkResponseCoordinator {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    BarkResponseCoordinator.class
            );

    private final ResponseProperties responseProperties;
    private final ResponseSoundPlayer responseSoundPlayer;
    private final RandomResponseSelector responseSelector;
    private final ResponseHistoryRepository historyRepository;

    private long barkingSessionStartMillis;
    private long lastBarkTimeMillis;

    private boolean firstResponsePlayed;
    private boolean secondResponsePlayed;
    private boolean prolongedResponsePlayed;

    public BarkResponseCoordinator(
            ResponseProperties responseProperties,
            ResponseSoundPlayer responseSoundPlayer,
            RandomResponseSelector responseSelector,
            ResponseHistoryRepository historyRepository
    ) {
        this.responseProperties =
                responseProperties;

        this.responseSoundPlayer =
                responseSoundPlayer;

        this.responseSelector =
                responseSelector;

        this.historyRepository =
                historyRepository;
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
            firstResponsePlayed =
                    playResponse(
                            ResponseLevel.FIRST,
                            "first-bark",
                            now
                    );

            if (firstResponsePlayed) {
                return;
            }
        }

        if (!prolongedResponsePlayed
                && sessionDuration
                >= responseProperties
                .getProlongedResponseDelayMillis()) {

            prolongedResponsePlayed =
                    playResponse(
                            ResponseLevel.PROLONGED,
                            "prolonged-barking",
                            now
                    );

            if (prolongedResponsePlayed) {
                return;
            }
        }

        if (!secondResponsePlayed
                && sessionDuration
                >= responseProperties
                .getSecondResponseDelayMillis()) {

            secondResponsePlayed =
                    playResponse(
                            ResponseLevel.SECOND,
                            "second-bark",
                            now
                    );
        }
    }

    public void onNoBarkDetected(long now) {
        if (!isSessionActive()) {
            return;
        }

        long quietDuration =
                now - lastBarkTimeMillis;

        if (quietDuration
                < responseProperties
                .getQuietResetMillis()) {

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

    private boolean playResponse(
            ResponseLevel responseLevel,
            String responseName,
            long playedAtMillis
    ) {
        Optional<Path> selectedSound;

        try {
            selectedSound =
                    responseSelector.select(
                            responseLevel
                    );

        } catch (IOException exception) {
            LOGGER.error(
                    "Unable to select a sound for "
                            + "{} response.",
                    responseName,
                    exception
            );

            return false;

        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Unexpected failure while selecting "
                            + "{} response.",
                    responseName,
                    exception
            );

            return false;
        }

        if (selectedSound.isEmpty()) {
            LOGGER.warn(
                    "Skipping {} response because no WAV "
                            + "files are available for level {}.",
                    responseName,
                    responseLevel
            );

            return false;
        }

        Path absoluteSoundPath =
                selectedSound
                        .get()
                        .toAbsolutePath()
                        .normalize();

        LOGGER.info(
                "Playing {} response: {}",
                responseName,
                absoluteSoundPath
        );

        try {
            responseSoundPlayer.play(
                    absoluteSoundPath
            );

        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Unable to play {} response.",
                    responseName,
                    exception
            );

            return false;
        }

        /*
         * Playback has succeeded at this point.
         * A persistence failure must not cause the
         * audible response to be repeated.
         */
        recordSuccessfulResponse(
                responseLevel,
                absoluteSoundPath,
                playedAtMillis
        );

        return true;
    }

    private void recordSuccessfulResponse(
            ResponseLevel responseLevel,
            Path soundFile,
            long playedAtMillis
    ) {
        Instant playedAt =
                Instant.ofEpochMilli(
                        playedAtMillis
                );

        try {
            historyRepository.record(
                    responseLevel,
                    soundFile,
                    playedAt
            );

            LOGGER.info(
                    "Response history recorded: "
                            + "time={}, level={}, file={}",
                    playedAt,
                    responseLevel,
                    soundFile
            );

        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Response played successfully, but its "
                            + "history event could not be saved. "
                            + "time={}, level={}, file={}",
                    playedAt,
                    responseLevel,
                    soundFile,
                    exception
            );

            return;
        }

        logUpdatedStatistics();
    }

    private void logUpdatedStatistics() {
        try {
            ResponseStatistics statistics =
                    historyRepository
                            .getStatistics();

            if (statistics == null) {
                return;
            }

            LOGGER.info(
                    "Persistent response totals: "
                            + "first={}, second={}, "
                            + "prolonged={}, overall={}",
                    statistics.firstResponses(),
                    statistics.secondResponses(),
                    statistics.prolongedResponses(),
                    statistics.overallResponses()
            );

        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Response was recorded, but updated "
                            + "statistics could not be read.",
                    exception
            );
        }
    }
}