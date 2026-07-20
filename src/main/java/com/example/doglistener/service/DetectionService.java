package com.example.doglistener.service;

import com.example.doglistener.audio.AudioChunk;
import com.example.doglistener.audio.MicrophoneCapture;
import com.example.doglistener.config.DetectorProperties;
import com.example.doglistener.ml.AudioConverter;
import com.example.doglistener.ml.InferenceEngine;
import com.example.doglistener.ml.Prediction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DetectionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DetectionService.class);

    private final MicrophoneCapture microphone;
    private final InferenceEngine inferenceEngine;
    private final DetectorProperties detectorProperties;
    private final BarkResponseCoordinator barkResponseCoordinator;

    private volatile boolean running;

    public DetectionService(
            MicrophoneCapture microphone,
            InferenceEngine inferenceEngine,
            DetectorProperties detectorProperties,
            BarkResponseCoordinator barkResponseCoordinator
    ) {
        this.microphone = microphone;
        this.inferenceEngine = inferenceEngine;
        this.detectorProperties = detectorProperties;
        this.barkResponseCoordinator =
                barkResponseCoordinator;
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

        try {
            microphone.start();

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

            microphone.stop();

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
                    inferenceEngine.predict(samples);

            LOGGER.info(
                    "Prediction: classId={}, "
                            + "label='{}', confidence={}",
                    prediction.classId(),
                    prediction.label(),
                    prediction.confidence()
            );

            processPrediction(prediction);

        } catch (Exception exception) {
            LOGGER.error(
                    "Failed to process audio chunk.",
                    exception
            );
        }
    }

    private void processPrediction(
            Prediction prediction
    ) {
        long now = System.currentTimeMillis();

        boolean barkDetected =
                prediction.isDogBark()
                        && prediction.confidence()
                        >= detectorProperties
                        .getConfidenceThreshold();

        if (barkDetected) {
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
}