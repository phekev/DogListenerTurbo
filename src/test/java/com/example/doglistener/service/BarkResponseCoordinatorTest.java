package com.example.doglistener.service;

import com.example.doglistener.config.ResponseProperties;
import com.example.doglistener.ml.Prediction;
import com.example.doglistener.sound.ResponseSoundPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BarkResponseCoordinatorTest {

    private static final String FIRST_SOUND =
            "sounds/response-1.wav";

    private static final String SECOND_SOUND =
            "sounds/response-2.wav";

    private static final String PROLONGED_SOUND =
            "sounds/response-3.wav";

    private static final long SECOND_DELAY_MILLIS =
            10_000L;

    private static final long PROLONGED_DELAY_MILLIS =
            60_000L;

    private static final long QUIET_RESET_MILLIS =
            10_000L;

    /*
     * Do not use zero as the session start time.
     *
     * BarkResponseCoordinator considers a session active when
     * barkingSessionStartMillis is greater than zero.
     */
    private static final long SESSION_START =
            1_000_000L;

    @Mock
    private ResponseSoundPlayer soundPlayer;

    private BarkResponseCoordinator coordinator;
    private Prediction barkPrediction;

    @BeforeEach
    void setUp() {
        ResponseProperties properties =
                new ResponseProperties();

        properties.setFirstSoundFile(FIRST_SOUND);
        properties.setSecondSoundFile(SECOND_SOUND);
        properties.setProlongedSoundFile(
                PROLONGED_SOUND
        );

        properties.setSecondResponseDelayMillis(
                SECOND_DELAY_MILLIS
        );

        properties.setProlongedResponseDelayMillis(
                PROLONGED_DELAY_MILLIS
        );

        properties.setQuietResetMillis(
                QUIET_RESET_MILLIS
        );

        coordinator =
                new BarkResponseCoordinator(
                        properties,
                        soundPlayer
                );

        barkPrediction =
                new Prediction(
                        -1,
                        "Dog Bark",
                        0.50f
                );
    }

    @Test
    void firstBarkStartsSessionAndPlaysFirstResponse() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        verify(soundPlayer).play(FIRST_SOUND);
        verifyNoMoreInteractions(soundPlayer);
    }

    @Test
    void secondResponsePlaysAtConfiguredDelay() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + SECOND_DELAY_MILLIS
                        - 1
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + SECOND_DELAY_MILLIS
        );

        verify(
                soundPlayer,
                times(1)
        ).play(FIRST_SOUND);

        verify(
                soundPlayer,
                times(1)
        ).play(SECOND_SOUND);

        verify(
                soundPlayer,
                never()
        ).play(PROLONGED_SOUND);

        verifyNoMoreInteractions(soundPlayer);
    }

    @Test
    void secondResponseIsNotRepeated() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + SECOND_DELAY_MILLIS
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + SECOND_DELAY_MILLIS
                        + 1_000L
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + SECOND_DELAY_MILLIS
                        + 5_000L
        );

        verify(
                soundPlayer,
                times(1)
        ).play(FIRST_SOUND);

        verify(
                soundPlayer,
                times(1)
        ).play(SECOND_SOUND);

        verify(
                soundPlayer,
                never()
        ).play(PROLONGED_SOUND);

        verifyNoMoreInteractions(soundPlayer);
    }

    @Test
    void prolongedResponsePlaysAtConfiguredDelay() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + SECOND_DELAY_MILLIS
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + PROLONGED_DELAY_MILLIS
        );

        InOrder playbackOrder =
                inOrder(soundPlayer);

        playbackOrder.verify(soundPlayer)
                .play(FIRST_SOUND);

        playbackOrder.verify(soundPlayer)
                .play(SECOND_SOUND);

        playbackOrder.verify(soundPlayer)
                .play(PROLONGED_SOUND);

        verifyNoMoreInteractions(soundPlayer);
    }

    @Test
    void prolongedResponseIsNotRepeated() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + SECOND_DELAY_MILLIS
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + PROLONGED_DELAY_MILLIS
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + PROLONGED_DELAY_MILLIS
                        + 1_000L
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + PROLONGED_DELAY_MILLIS
                        + 5_000L
        );

        verify(
                soundPlayer,
                times(1)
        ).play(FIRST_SOUND);

        verify(
                soundPlayer,
                times(1)
        ).play(SECOND_SOUND);

        verify(
                soundPlayer,
                times(1)
        ).play(PROLONGED_SOUND);

        verifyNoMoreInteractions(soundPlayer);
    }

    @Test
    void quietPeriodShorterThanResetDoesNotResetSession() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        coordinator.onNoBarkDetected(
                SESSION_START
                        + QUIET_RESET_MILLIS
                        - 1_000L
        );

        /*
         * This bark occurs before both the quiet-reset threshold
         * and the second-response threshold.
         */
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + QUIET_RESET_MILLIS
                        - 999L
        );

        verify(
                soundPlayer,
                times(1)
        ).play(FIRST_SOUND);

        verify(
                soundPlayer,
                never()
        ).play(SECOND_SOUND);

        verify(
                soundPlayer,
                never()
        ).play(PROLONGED_SOUND);

        verifyNoMoreInteractions(soundPlayer);
    }

    @Test
    void quietPeriodAtResetThresholdStartsNewSession() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        coordinator.onNoBarkDetected(
                SESSION_START
                        + QUIET_RESET_MILLIS
        );

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
                        + QUIET_RESET_MILLIS
                        + 1L
        );

        verify(
                soundPlayer,
                times(2)
        ).play(FIRST_SOUND);

        verify(
                soundPlayer,
                never()
        ).play(SECOND_SOUND);

        verify(
                soundPlayer,
                never()
        ).play(PROLONGED_SOUND);

        verifyNoMoreInteractions(soundPlayer);
    }

    @Test
    void explicitResetAllowsFirstResponseAgain() {
        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START
        );

        coordinator.reset();

        coordinator.onBarkDetected(
                barkPrediction,
                SESSION_START + 1L
        );

        verify(
                soundPlayer,
                times(2)
        ).play(FIRST_SOUND);

        verifyNoMoreInteractions(soundPlayer);
    }
}