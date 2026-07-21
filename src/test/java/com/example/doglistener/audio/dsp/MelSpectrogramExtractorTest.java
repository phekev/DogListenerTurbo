package com.example.doglistener.audio.dsp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MelSpectrogramExtractorTest {

    private static final float COMPARISON_TOLERANCE =
            0.000001f;

    private MelSpectrogramExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor =
                new MelSpectrogramExtractor();
    }

    @Test
    void createsExpectedSpectrogramDimensions() {
        float[] samples =
                new float[DspConstants.PATCH_SAMPLES];

        float[][] result =
                extractor.extract(samples);

        assertEquals(
                DspConstants.FRAMES,
                result.length
        );

        for (float[] frame : result) {
            assertEquals(
                    DspConstants.MEL_BINS,
                    frame.length
            );
        }
    }

    @Test
    void createsIndependentFrameArrays() {
        float[] samples =
                new float[DspConstants.PATCH_SAMPLES];

        float[][] result =
                extractor.extract(samples);

        for (int frame = 1;
             frame < result.length;
             frame++) {

            assertNotSame(
                    result[frame - 1],
                    result[frame]
            );
        }
    }

    @Test
    void rejectsNullSamples() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> extractor.extract(null)
                );

        assertEquals(
                "Samples must not be null.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsIncorrectSampleCount() {
        float[] samples =
                new float[
                        DspConstants.PATCH_SAMPLES - 1
                        ];

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> extractor.extract(samples)
                );

        assertEquals(
                "Expected "
                        + DspConstants.PATCH_SAMPLES
                        + " samples.",
                exception.getMessage()
        );
    }

    @Test
    void producesOnlyFiniteValuesForSilence() {
        float[] samples =
                new float[DspConstants.PATCH_SAMPLES];

        float[][] result =
                extractor.extract(samples);

        for (float[] frame : result) {
            for (float value : frame) {
                assertTrue(
                        Float.isFinite(value),
                        "Spectrogram contained "
                                + "a non-finite value: "
                                + value
                );
            }
        }
    }

    @Test
    void producesDeterministicOutput() {
        float[] samples =
                createTestWaveform();

        float[][] firstResult =
                extractor.extract(samples);

        float[][] secondResult =
                extractor.extract(samples);

        assertSpectrogramEquals(
                firstResult,
                secondResult
        );
    }

    @Test
    void doesNotModifyInputSamples() {
        float[] samples =
                createTestWaveform();

        float[] originalCopy =
                Arrays.copyOf(
                        samples,
                        samples.length
                );

        extractor.extract(samples);

        assertArrayEquals(
                originalCopy,
                samples
        );
    }

    @Test
    void differentWaveformsProduceDifferentResults() {
        float[] silence =
                new float[DspConstants.PATCH_SAMPLES];

        float[] impulse =
                new float[DspConstants.PATCH_SAMPLES];

        impulse[DspConstants.WINDOW_SIZE / 2] =
                1.0f;

        float[][] silenceResult =
                extractor.extract(silence);

        float[][] impulseResult =
                extractor.extract(impulse);

        assertFalse(
                spectrogramsAreEqual(
                        silenceResult,
                        impulseResult
                ),
                "An impulse and silence produced "
                        + "identical spectrograms."
        );
    }

    private static float[] createTestWaveform() {
        float[] samples =
                new float[DspConstants.PATCH_SAMPLES];

        for (int index = 0;
             index < samples.length;
             index++) {

            samples[index] =
                    (float) (
                            0.60
                                    * Math.sin(
                                    index * 0.037
                            )
                    );
        }

        return samples;
    }

    private static void assertSpectrogramEquals(
            float[][] expected,
            float[][] actual
    ) {
        assertEquals(
                expected.length,
                actual.length
        );

        for (int frame = 0;
             frame < expected.length;
             frame++) {

            assertArrayEquals(
                    expected[frame],
                    actual[frame],
                    COMPARISON_TOLERANCE
            );
        }
    }

    private static boolean spectrogramsAreEqual(
            float[][] first,
            float[][] second
    ) {
        if (first.length != second.length) {
            return false;
        }

        for (int frame = 0;
             frame < first.length;
             frame++) {

            if (first[frame].length
                    != second[frame].length) {

                return false;
            }

            for (int bin = 0;
                 bin < first[frame].length;
                 bin++) {

                float difference =
                        Math.abs(
                                first[frame][bin]
                                        - second[frame][bin]
                        );

                if (difference
                        > COMPARISON_TOLERANCE) {

                    return false;
                }
            }
        }

        return true;
    }
}