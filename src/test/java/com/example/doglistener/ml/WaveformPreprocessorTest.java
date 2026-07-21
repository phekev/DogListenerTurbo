
package com.example.doglistener.ml;

import com.example.doglistener.audio.dsp.DspConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WaveformPreprocessorTest {

    private WaveformPreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor =
                new WaveformPreprocessor();
    }

    @Test
    void returnsOriginalArrayWhenLengthIsCorrect() {
        float[] audio =
                new float[DspConstants.PATCH_SAMPLES];

        audio[0] = 0.25f;
        audio[audio.length - 1] = -0.50f;

        float[] result =
                preprocessor.prepare(audio);

        assertSame(audio, result);

        assertEquals(
                DspConstants.PATCH_SAMPLES,
                result.length
        );
    }

    @Test
    void zeroPadsAudioWhenTooShort() {
        int originalLength =
                DspConstants.PATCH_SAMPLES - 100;

        float[] audio =
                new float[originalLength];

        Arrays.fill(audio, 0.75f);

        float[] result =
                preprocessor.prepare(audio);

        assertNotSame(audio, result);

        assertEquals(
                DspConstants.PATCH_SAMPLES,
                result.length
        );

        for (int index = 0;
             index < originalLength;
             index++) {

            assertEquals(
                    0.75f,
                    result[index]
            );
        }

        for (int index = originalLength;
             index < result.length;
             index++) {

            assertEquals(
                    0.0f,
                    result[index]
            );
        }
    }

    @Test
    void trimsAudioWhenTooLong() {
        int requiredSamples =
                DspConstants.PATCH_SAMPLES;

        float[] audio =
                new float[requiredSamples + 100];

        for (int index = 0;
             index < audio.length;
             index++) {

            audio[index] = index;
        }

        float[] result =
                preprocessor.prepare(audio);

        assertNotSame(audio, result);

        assertEquals(
                requiredSamples,
                result.length
        );

        for (int index = 0;
             index < requiredSamples;
             index++) {

            assertEquals(
                    audio[index],
                    result[index]
            );
        }
    }

    @Test
    void preservesShortAudioSamplesBeforePadding() {
        float[] audio = {
                0.10f,
                -0.20f,
                0.30f,
                -0.40f
        };

        float[] result =
                preprocessor.prepare(audio);

        assertArrayEquals(
                audio,
                Arrays.copyOf(
                        result,
                        audio.length
                )
        );
    }

    @Test
    void doesNotModifyShortInputArray() {
        float[] audio = {
                0.10f,
                0.20f,
                0.30f
        };

        float[] originalCopy =
                Arrays.copyOf(
                        audio,
                        audio.length
                );

        preprocessor.prepare(audio);

        assertArrayEquals(
                originalCopy,
                audio
        );
    }

    @Test
    void doesNotModifyLongInputArray() {
        float[] audio =
                new float[
                        DspConstants.PATCH_SAMPLES + 10
                        ];

        Arrays.fill(audio, 0.60f);

        float[] originalCopy =
                Arrays.copyOf(
                        audio,
                        audio.length
                );

        preprocessor.prepare(audio);

        assertArrayEquals(
                originalCopy,
                audio
        );
    }

    @Test
    void rejectsNullAudio() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> preprocessor.prepare(null)
                );

        assertEquals(
                "Audio must not be null.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsEmptyAudio() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> preprocessor.prepare(
                                new float[0]
                        )
                );

        assertEquals(
                "Audio must not be empty.",
                exception.getMessage()
        );
    }
}