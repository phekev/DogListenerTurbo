package com.example.doglistener.audio;

import com.example.doglistener.config.AudioProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

@Component
public class MicrophoneCapture {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    MicrophoneCapture.class
            );

    private final AudioProperties config;
    private final AudioLineProvider lineProvider;

    private TargetDataLine microphone;
    private int bytesPerChunk;

    public MicrophoneCapture(
            AudioProperties config,
            AudioLineProvider lineProvider
    ) {
        this.config = config;
        this.lineProvider = lineProvider;
    }

    public void start() throws Exception {
        if (microphone != null) {
            throw new IllegalStateException(
                    "Microphone is already started."
            );
        }

        AudioFormat format =
                AudioFormatFactory.create(config);

        DataLine.Info lineInfo =
                new DataLine.Info(
                        TargetDataLine.class,
                        format
                );

        if (!lineProvider.isLineSupported(
                lineInfo
        )) {
            throw new IllegalStateException(
                    "Microphone format not supported."
            );
        }

        int calculatedBytesPerChunk =
                calculateBytesPerChunk();

        TargetDataLine openedMicrophone =
                lineProvider.getTargetDataLine(
                        lineInfo
                );

        try {
            openedMicrophone.open(
                    format,
                    config.getBufferSize()
            );

            openedMicrophone.start();

        } catch (Exception exception) {
            openedMicrophone.close();
            throw exception;
        }

        microphone = openedMicrophone;
        bytesPerChunk =
                calculatedBytesPerChunk;

        LOGGER.info("Microphone started.");

        LOGGER.info(
                "Chunk size = {} bytes",
                bytesPerChunk
        );
    }

    public AudioChunk readChunk() {
        TargetDataLine activeMicrophone =
                microphone;

        if (activeMicrophone == null
                || bytesPerChunk <= 0) {

            throw new IllegalStateException(
                    "Microphone is not started."
            );
        }

        byte[] buffer =
                new byte[bytesPerChunk];

        int offset = 0;

        while (offset < buffer.length) {
            int bytesRead =
                    activeMicrophone.read(
                            buffer,
                            offset,
                            buffer.length - offset
                    );

            if (bytesRead <= 0) {
                throw new IllegalStateException(
                        "Microphone stopped before "
                                + "the audio chunk was complete."
                );
            }

            offset += bytesRead;
        }

        long timestamp =
                System.nanoTime();

        return new AudioChunk(
                buffer,
                timestamp
        );
    }

    private int calculateBytesPerChunk() {
        int sampleSize =
                config.getSampleSize();

        if (sampleSize <= 0
                || sampleSize % 8 != 0) {

            throw new IllegalStateException(
                    "Audio sample size must be "
                            + "a positive multiple of 8."
            );
        }

        double calculatedSize =
                config.getSampleRate()
                        * (config.getChunkMillis()
                        / 1000.0)
                        * config.getChannels()
                        * (sampleSize / 8.0);

        if (!Double.isFinite(calculatedSize)
                || calculatedSize <= 0.0
                || calculatedSize
                > Integer.MAX_VALUE) {

            throw new IllegalStateException(
                    "Invalid audio chunk size: "
                            + calculatedSize
            );
        }

        return (int) Math.round(
                calculatedSize
        );
    }

    @PreDestroy
    public void stop() {
        TargetDataLine activeMicrophone =
                microphone;

        microphone = null;
        bytesPerChunk = 0;

        if (activeMicrophone == null) {
            return;
        }

        try {
            activeMicrophone.stop();
        } finally {
            activeMicrophone.close();
        }

        LOGGER.info("Microphone stopped.");
    }
}