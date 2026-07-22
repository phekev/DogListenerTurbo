package com.example.doglistener.service;

import com.example.doglistener.audio.AudioChunk;
import com.example.doglistener.audio.MicrophoneCapture;
import com.example.doglistener.config.DetectorProperties;
import com.example.doglistener.ml.AudioConverter;
import com.example.doglistener.ml.InferenceEngine;
import com.example.doglistener.ml.Prediction;
import com.example.doglistener.status.RuntimeStatusStore;
import com.example.doglistener.web.ConfidenceSampleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DetectionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    DetectionService.class
            );

    private final MicrophoneCapture microphone;
    private final InferenceEngine inferenceEngine;
    private final DetectorProperties detectorProperties;

    private final BarkResponseCoordinator
            barkResponseCoordinator;

    private final ConfidenceSampleStore
            confidenceSampleStore;

    private final RuntimeStatusStore
            runtimeStatusStore;

    private volatile boolean running;

    public DetectionService(
            MicrophoneCapture microphone,
            InferenceEngine inferenceEngine,
            DetectorProperties detectorProperties,
            BarkResponseCoordinator barkResponseCoordinator,
            ConfidenceSampleStore confidenceSampleStore,
            RuntimeStatusStore runtimeStatusStore
    ) {
        this.microphone =
                microphone;

        this.inferenceEngine =
                inferenceEngine;

        this.detectorProperties =
                detectorProperties;

        this.barkResponseCoordinator =
                barkResponseCoordinator;

        this.confidenceSampleStore =
                confidenceSampleStore;

        this.runtimeStatusStore =
                runtimeStatusStore;
    }

    public void start() throws Exception {
        if (running) {
            LOGGER.warn(
                    "Detection service is already running."
            );

            return;
        }

        running = true;

        barkResponseCoordinator.reset();

        updateRuntimeStatus(
                () -> runtimeStatusStore
                        .markDetectionStarted(
                                detectorProperties
                                        .getConfidenceThreshold()
                        ),
                "mark detection as started"
        );

        try {
            microphone.start();

            updateRuntimeStatus(
                    runtimeStatusStore
                            ::markMicrophoneStarted,
                    "mark microphone as started"
            );

            LOGGER.info(
                    "Detection started. "
                            + "Confidence threshold={}",
                    detectorProperties
                            .getConfidenceThreshold()
            );

            while (running
                    && !Thread.currentThread()
                    .isInterrupted()) {

                processNextChunk();
            }

        } finally {
            running = false;

            barkResponseCoordinator.reset();

            try {
                microphone.stop();

            } finally {
                updateRuntimeStatus(
                        runtimeStatusStore
                                ::markMicrophoneStopped,
                        "mark microphone as stopped"
                );

                updateRuntimeStatus(
                        runtimeStatusStore
                                ::markDetectionStopped,
                        "mark detection as stopped"
                );
            }

            LOGGER.info(
                    "Detection service stopped."
            );
        }
    }

    public void stop() {
        running = false;
    }

    private void processNextChunk() {
        try {
            LOGGER.info(
                    "Waiting for audio chunk..."
            );

            AudioChunk chunk =
                    microphone.readChunk();

            Instant audioChunkTime =
                    Instant.now();

            updateRuntimeStatus(
                    () -> runtimeStatusStore
                            .markAudioChunkReceived(
                                    audioChunkTime
                            ),
                    "record audio chunk time"
            );

            LOGGER.info(
                    "Audio chunk received: {} bytes",
                    chunk.getPcm().length
            );

            float[] samples =
                    AudioConverter.pcm16ToFloat(
                            chunk.getPcm()
                    );

            LOGGER.info(
                    "Converted audio: {} samples",
                    samples.length
            );

            Prediction prediction =
                    inferenceEngine.predict(
                            samples
                    );

            Instant predictionTime =
                    Instant.now();

            LOGGER.info(
                    "Prediction: classId={}, "
                            + "label='{}', confidence={}",
                    prediction.classId(),
                    prediction.label(),
                    prediction.confidence()
            );

            recordConfidence(
                    prediction.confidence()
            );

            updateRuntimeStatus(
                    () -> runtimeStatusStore
                            .markPrediction(
                                    prediction.confidence(),
                                    predictionTime
                            ),
                    "record prediction status"
            );

            processPrediction(
                    prediction,
                    predictionTime
            );

        } catch (Exception exception) {
            LOGGER.error(
                    "Failed to process audio chunk.",
                    exception
            );
        }
    }

    private void recordConfidence(
            float confidence
    ) {
        try {
            confidenceSampleStore.record(
                    confidence
            );

        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Unable to record confidence sample: {}",
                    confidence,
                    exception
            );
        }
    }

    private void processPrediction(
            Prediction prediction,
            Instant predictionTime
    ) {
        long now =
                predictionTime.toEpochMilli();

        boolean barkDetected =
                prediction.isDogBark()
                        && prediction.confidence()
                        >= detectorProperties
                        .getConfidenceThreshold();

        if (barkDetected) {
            updateRuntimeStatus(
                    () -> runtimeStatusStore
                            .markBarkDetected(
                                    predictionTime
                            ),
                    "record bark detection time"
            );

            barkResponseCoordinator.onBarkDetected(
                    prediction,
                    now
            );

        } else {
            barkResponseCoordinator.onNoBarkDetected(
                    now
            );
        }
    }

    private void updateRuntimeStatus(
            Runnable update,
            String description
    ) {
        try {
            update.run();

        } catch (RuntimeException exception) {
            /*
             * Dashboard telemetry must never interrupt
             * microphone capture or bark detection.
             */
            LOGGER.warn(
                    "Unable to {}.",
                    description,
                    exception
            );
        }
    }
}