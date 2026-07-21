package com.example.doglistener.service;

import com.example.doglistener.audio.AudioChunk;
import com.example.doglistener.audio.MicrophoneCapture;
import com.example.doglistener.config.DetectorProperties;
import com.example.doglistener.ml.InferenceEngine;
import com.example.doglistener.ml.Prediction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionServiceTest {

    private static final double CONFIDENCE_THRESHOLD =
            0.20d;

    @Mock
    private MicrophoneCapture microphone;

    @Mock
    private InferenceEngine inferenceEngine;

    @Mock
    private DetectorProperties detectorProperties;

    @Mock
    private BarkResponseCoordinator
            barkResponseCoordinator;

    private DetectionService detectionService;

    @BeforeEach
    void setUp() {
        when(
                detectorProperties
                        .getConfidenceThreshold()
        ).thenReturn(CONFIDENCE_THRESHOLD);

        detectionService =
                new DetectionService(
                        microphone,
                        inferenceEngine,
                        detectorProperties,
                        barkResponseCoordinator
                );
    }

    @Test
    void qualifyingDogBarkIsSentToCoordinator()
            throws Exception {

        Prediction prediction =
                new Prediction(
                        -1,
                        "Dog Bark",
                        0.50f
                );

        configureSingleChunkRun(prediction);

        detectionService.start();

        verify(
                barkResponseCoordinator
        ).onBarkDetected(
                eq(prediction),
                anyLong()
        );

        verify(
                barkResponseCoordinator,
                never()
        ).onNoBarkDetected(anyLong());
    }

    @Test
    void lowConfidenceDogBarkIsTreatedAsNoBark()
            throws Exception {

        Prediction prediction =
                new Prediction(
                        -1,
                        "Dog Bark",
                        0.10f
                );

        configureSingleChunkRun(prediction);

        detectionService.start();

        verify(
                barkResponseCoordinator
        ).onNoBarkDetected(anyLong());

        verify(
                barkResponseCoordinator,
                never()
        ).onBarkDetected(
                any(Prediction.class),
                anyLong()
        );
    }

    @Test
    void nonBarkPredictionIsTreatedAsNoBark()
            throws Exception {

        Prediction prediction =
                new Prediction(
                        0,
                        "Speech",
                        0.90f
                );

        configureSingleChunkRun(prediction);

        detectionService.start();

        verify(
                barkResponseCoordinator
        ).onNoBarkDetected(anyLong());

        verify(
                barkResponseCoordinator,
                never()
        ).onBarkDetected(
                any(Prediction.class),
                anyLong()
        );
    }

    @Test
    void confidenceEqualToThresholdIsAccepted()
            throws Exception {

        Prediction prediction =
                new Prediction(
                        -1,
                        "Dog Bark",
                        (float) CONFIDENCE_THRESHOLD
                );

        configureSingleChunkRun(prediction);

        detectionService.start();

        verify(
                barkResponseCoordinator
        ).onBarkDetected(
                eq(prediction),
                anyLong()
        );
    }

    @Test
    void startControlsMicrophoneLifecycle()
            throws Exception {

        Prediction prediction =
                new Prediction(
                        0,
                        "Silence",
                        0.01f
                );

        configureSingleChunkRun(prediction);

        detectionService.start();

        verify(microphone).start();
        verify(microphone).stop();
    }

    @Test
    void coordinatorIsResetAtStartAndShutdown()
            throws Exception {

        Prediction prediction =
                new Prediction(
                        0,
                        "Silence",
                        0.01f
                );

        configureSingleChunkRun(prediction);

        detectionService.start();

        verify(
                barkResponseCoordinator,
                times(2)
        ).reset();
    }

    private void configureSingleChunkRun(
            Prediction prediction
    ) throws Exception {

        AudioChunk chunk =
                mock(AudioChunk.class);

        /*
         * 32,000 bytes represents one second of:
         *
         * 16,000 Hz
         * mono
         * 16-bit PCM
         */
        when(chunk.getPcm())
                .thenReturn(new byte[32_000]);

        when(microphone.readChunk())
                .thenReturn(chunk);

        /*
         * Stop the service while processing this prediction.
         * The current chunk completes normally, and the loop exits
         * before requesting another microphone chunk.
         */
        doAnswer(invocation -> {
            detectionService.stop();
            return prediction;
        }).when(inferenceEngine)
                .predict(any(float[].class));
    }
}