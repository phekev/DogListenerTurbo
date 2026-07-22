package com.example.doglistener.service;

import com.example.doglistener.audio.RandomResponseSelector;
import com.example.doglistener.audio.ResponseLevel;
import com.example.doglistener.config.ResponseProperties;
import com.example.doglistener.ml.Prediction;
import com.example.doglistener.sound.ResponseSoundPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import com.example.doglistener.history.ResponseHistoryException;
import com.example.doglistener.history.ResponseHistoryRepository;
import com.example.doglistener.history.ResponseStatistics;

import java.time.Instant;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarkResponseCoordinatorTest {

    private static final long SESSION_START =
            1_000L;

    private static final Prediction BARK =
            new Prediction(
                    -1,
                    "Dog Bark",
                    0.80f
            );

    @Mock
    private ResponseSoundPlayer soundPlayer;

    @Mock
    private ResponseHistoryRepository historyRepository;

    @Mock
    private RandomResponseSelector responseSelector;

    private ResponseProperties properties;
    private BarkResponseCoordinator coordinator;

    @BeforeEach
    void setUp() {
        properties = new ResponseProperties();

        properties.setSecondResponseDelayMillis(
                10_000L
        );

        properties.setProlongedResponseDelayMillis(
                60_000L
        );

        properties.setQuietResetMillis(
                10_000L
        );

        coordinator =
                new BarkResponseCoordinator(
                        properties,
                        soundPlayer,
                        responseSelector,
                        historyRepository
                );
    }

    @Test
    void firstBarkPlaysRandomFirstResponse()
            throws IOException {

        Path sound =
                Path.of("sounds", "response_one", "hey.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(sound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        verify(soundPlayer).play(
                absolute(sound)
        );
    }
    @Test
    void successfulFirstResponseIsPersisted()
            throws IOException {

        Path sound =
                Path.of(
                        "response_one",
                        "hey.wav"
                );

        Path absoluteSound =
                absolute(sound);

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(sound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        verify(historyRepository).record(
                ResponseLevel.FIRST,
                absoluteSound,
                Instant.ofEpochMilli(
                        SESSION_START
                )
        );
    }
    @Test
    void successfulSecondResponseIsPersisted()
            throws IOException {

        Path firstSound =
                Path.of(
                        "response_one",
                        "hey.wav"
                );

        Path secondSound =
                Path.of(
                        "response_two",
                        "enough.wav"
                );

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(firstSound)
        );

        when(
                responseSelector.select(
                        ResponseLevel.SECOND
                )
        ).thenReturn(
                Optional.of(secondSound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 10_000L
        );

        verify(historyRepository).record(
                ResponseLevel.SECOND,
                absolute(secondSound),
                Instant.ofEpochMilli(
                        SESSION_START + 10_000L
                )
        );
    }
    @Test
    void successfulProlongedResponseIsPersisted()
            throws IOException {

        Path firstSound =
                Path.of(
                        "response_one",
                        "hey.wav"
                );

        Path prolongedSound =
                Path.of(
                        "response_three",
                        "final-warning.wav"
                );

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(firstSound)
        );

        when(
                responseSelector.select(
                        ResponseLevel.PROLONGED
                )
        ).thenReturn(
                Optional.of(prolongedSound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 60_000L
        );

        verify(historyRepository).record(
                ResponseLevel.PROLONGED,
                absolute(prolongedSound),
                Instant.ofEpochMilli(
                        SESSION_START + 60_000L
                )
        );
    }
    @Test
    void playbackFailureIsNotPersisted()
            throws IOException {

        Path sound =
                Path.of(
                        "response_one",
                        "broken.wav"
                );

        Path absoluteSound =
                absolute(sound);

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(sound)
        );

        doThrow(
                new IllegalStateException(
                        "Simulated playback failure"
                )
        ).when(soundPlayer)
                .play(absoluteSound);

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        verify(
                historyRepository,
                never()
        ).record(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
    @Test
    void historyFailureDoesNotReplaySound()
            throws IOException {

        Path sound =
                Path.of(
                        "response_one",
                        "hey.wav"
                );

        Path absoluteSound =
                absolute(sound);

        Instant playedAt =
                Instant.ofEpochMilli(
                        SESSION_START
                );

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(sound)
        );

        doThrow(
                new ResponseHistoryException(
                        "Simulated database failure",
                        new IllegalStateException(
                                "Database unavailable"
                        )
                )
        ).when(historyRepository)
                .record(
                        ResponseLevel.FIRST,
                        absoluteSound,
                        playedAt
                );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        /*
         * Another bark in the same session must not
         * replay response one merely because logging
         * failed.
         */
        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 1_000L
        );

        verify(
                soundPlayer,
                times(1)
        ).play(absoluteSound);

        verify(
                responseSelector,
                times(1)
        ).select(
                ResponseLevel.FIRST
        );
    }
    @Test
    void logsUpdatedPersistentStatistics()
            throws IOException {

        Path sound =
                Path.of(
                        "response_one",
                        "hey.wav"
                );

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(sound)
        );

        when(
                historyRepository.getStatistics()
        ).thenReturn(
                new ResponseStatistics(
                        3L,
                        2L,
                        1L,
                        6L
                )
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        verify(historyRepository)
                .getStatistics();
    }
    @Test
    void continuedBarkingPlaysSecondResponse()
            throws IOException {

        Path first =
                Path.of("response_one", "hey.wav");

        Path second =
                Path.of("response_two", "stop.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(first)
        );

        when(
                responseSelector.select(
                        ResponseLevel.SECOND
                )
        ).thenReturn(
                Optional.of(second)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 10_000L
        );

        verify(soundPlayer).play(
                absolute(first)
        );

        verify(soundPlayer).play(
                absolute(second)
        );
    }

    @Test
    void prolongedBarkingPlaysThirdResponseFirst()
            throws IOException {

        Path first =
                Path.of("response_one", "hey.wav");

        Path prolonged =
                Path.of("response_three", "enough.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(first)
        );

        when(
                responseSelector.select(
                        ResponseLevel.PROLONGED
                )
        ).thenReturn(
                Optional.of(prolonged)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 60_000L
        );

        verify(soundPlayer).play(
                absolute(prolonged)
        );

        verify(
                responseSelector,
                never()
        ).select(
                ResponseLevel.SECOND
        );
    }

    @Test
    void doesNotRepeatFirstResponseWithinSession()
            throws IOException {

        Path first =
                Path.of("response_one", "hey.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(first)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 5_000L
        );

        verify(
                responseSelector,
                times(1)
        ).select(
                ResponseLevel.FIRST
        );

        verify(
                soundPlayer,
                times(1)
        ).play(
                absolute(first)
        );
    }

    @Test
    void quietPeriodResetsBarkingSession()
            throws IOException {

        Path firstSound =
                Path.of("response_one", "hey.wav");

        Path nextSessionSound =
                Path.of("response_one", "stop.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(firstSound),
                Optional.of(nextSessionSound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onNoBarkDetected(
                SESSION_START + 10_000L
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 11_000L
        );

        verify(soundPlayer).play(
                absolute(firstSound)
        );

        verify(soundPlayer).play(
                absolute(nextSessionSound)
        );
    }

    @Test
    void shortQuietPeriodDoesNotResetSession()
            throws IOException {

        Path sound =
                Path.of("response_one", "hey.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(sound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onNoBarkDetected(
                SESSION_START + 5_000L
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 6_000L
        );

        verify(
                responseSelector,
                times(1)
        ).select(
                ResponseLevel.FIRST
        );
    }

    @Test
    void emptyFirstFolderIsRetriedOnNextBark()
            throws IOException {

        Path sound =
                Path.of("response_one", "hey.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.empty(),
                Optional.of(sound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 1_000L
        );

        verify(
                responseSelector,
                times(2)
        ).select(
                ResponseLevel.FIRST
        );

        verify(soundPlayer).play(
                absolute(sound)
        );
    }

    @Test
    void selectionFailureDoesNotEndDetection()
            throws IOException {

        Path sound =
                Path.of("response_one", "hey.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenThrow(
                new IOException(
                        "Simulated filesystem failure"
                )
        ).thenReturn(
                Optional.of(sound)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 1_000L
        );

        verify(soundPlayer).play(
                absolute(sound)
        );
    }

    @Test
    void playbackFailureAllowsRetry()
            throws IOException {

        Path sound =
                Path.of("response_one", "hey.wav");

        Path absoluteSound =
                absolute(sound);

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(sound)
        );

        doThrow(
                new IllegalStateException(
                        "Simulated playback failure"
                )
        ).doNothing()
                .when(soundPlayer)
                .play(absoluteSound);

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 1_000L
        );

        verify(
                soundPlayer,
                times(2)
        ).play(absoluteSound);
    }

    @Test
    void missingProlongedSoundFallsBackToSecond()
            throws IOException {

        Path first =
                Path.of("response_one", "hey.wav");

        Path second =
                Path.of("response_two", "stop.wav");

        when(
                responseSelector.select(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                Optional.of(first)
        );

        when(
                responseSelector.select(
                        ResponseLevel.PROLONGED
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                responseSelector.select(
                        ResponseLevel.SECOND
                )
        ).thenReturn(
                Optional.of(second)
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START
        );

        coordinator.onBarkDetected(
                BARK,
                SESSION_START + 60_000L
        );

        verify(soundPlayer).play(
                absolute(second)
        );
    }

    private static Path absolute(Path path) {
        return path
                .toAbsolutePath()
                .normalize();
    }
}