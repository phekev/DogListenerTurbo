package com.example.doglistener.audio;

import com.example.doglistener.config.AudioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioFormatFactoryTest {

    private AudioProperties config;

    @BeforeEach
    void setUp() {
        config = new AudioProperties();

        config.setSampleRate(16_000.0f);
        config.setSampleSize(16);
        config.setChannels(1);
        config.setSigned(true);
        config.setBigEndian(false);
    }

    @Test
    void createsConfiguredAudioFormat() {
        AudioFormat format =
                AudioFormatFactory.create(config);

        assertEquals(
                16_000.0f,
                format.getSampleRate()
        );

        assertEquals(
                16,
                format.getSampleSizeInBits()
        );

        assertEquals(
                1,
                format.getChannels()
        );

        assertEquals(
                AudioFormat.Encoding.PCM_SIGNED,
                format.getEncoding()
        );

        assertFalse(
                format.isBigEndian()
        );
    }

    @Test
    void calculatesExpectedFrameSize() {
        AudioFormat format =
                AudioFormatFactory.create(config);

        /*
         * One channel × two bytes per sample.
         */
        assertEquals(
                2,
                format.getFrameSize()
        );
    }

    @Test
    void usesSampleRateAsFrameRate() {
        AudioFormat format =
                AudioFormatFactory.create(config);

        assertEquals(
                16_000.0f,
                format.getFrameRate()
        );
    }

    @Test
    void createsUnsignedAudioFormat() {
        config.setSigned(false);

        AudioFormat format =
                AudioFormatFactory.create(config);

        assertEquals(
                AudioFormat.Encoding.PCM_UNSIGNED,
                format.getEncoding()
        );
    }

    @Test
    void createsBigEndianAudioFormat() {
        config.setBigEndian(true);

        AudioFormat format =
                AudioFormatFactory.create(config);

        assertTrue(
                format.isBigEndian()
        );
    }

    @Test
    void calculatesStereoFrameSize() {
        config.setChannels(2);
        config.setSampleSize(16);

        AudioFormat format =
                AudioFormatFactory.create(config);

        /*
         * Two channels × two bytes per sample.
         */
        assertEquals(
                4,
                format.getFrameSize()
        );
    }

    @Test
    void rejectsNullConfiguration() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> AudioFormatFactory.create(null)
                );

        assertEquals(
                "Audio configuration must not be null.",
                exception.getMessage()
        );
    }
}