package com.example.doglistener.audio.dsp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FFTProcessorTest {

    private static final float TOLERANCE =
            0.0001f;

    private FFTProcessor fftProcessor;

    @BeforeEach
    void setUp() {
        fftProcessor =
                new FFTProcessor();
    }

    @Test
    void createsExpectedNumberOfFftBins() {
        float[] frame =
                new float[DspConstants.WINDOW_SIZE];

        float[] result =
                fftProcessor.powerSpectrum(frame);

        assertEquals(
                DspConstants.FFT_BINS,
                result.length
        );
    }

    @Test
    void silenceProducesZeroPower() {
        float[] frame =
                new float[DspConstants.WINDOW_SIZE];

        float[] result =
                fftProcessor.powerSpectrum(frame);

        for (float value : result) {
            assertEquals(
                    0.0f,
                    value,
                    TOLERANCE
            );
        }
    }

    @Test
    void unitImpulseProducesFlatPowerSpectrum() {
        float[] frame =
                new float[DspConstants.WINDOW_SIZE];

        frame[0] = 1.0f;

        float[] result =
                fftProcessor.powerSpectrum(frame);

        for (float value : result) {
            assertEquals(
                    1.0f,
                    value,
                    TOLERANCE
            );
        }
    }

    @Test
    void impulseAmplitudeIsSquaredInPowerSpectrum() {
        float[] frame =
                new float[DspConstants.WINDOW_SIZE];

        frame[0] = 2.0f;

        float[] result =
                fftProcessor.powerSpectrum(frame);

        for (float value : result) {
            assertEquals(
                    4.0f,
                    value,
                    TOLERANCE
            );
        }
    }

    @Test
    void producesOnlyFiniteNonNegativeValues() {
        float[] frame =
                createTestFrame();

        float[] result =
                fftProcessor.powerSpectrum(frame);

        for (float value : result) {
            assertTrue(
                    Float.isFinite(value),
                    "Power spectrum contained "
                            + "a non-finite value: "
                            + value
            );

            assertTrue(
                    value >= 0.0f,
                    "Power spectrum contained "
                            + "a negative value: "
                            + value
            );
        }
    }

    @Test
    void producesDeterministicOutput() {
        float[] frame =
                createTestFrame();

        float[] firstResult =
                fftProcessor.powerSpectrum(frame);

        float[] secondResult =
                fftProcessor.powerSpectrum(frame);

        assertArrayEquals(
                firstResult,
                secondResult,
                TOLERANCE
        );
    }

    @Test
    void doesNotModifyInputFrame() {
        float[] frame =
                createTestFrame();

        float[] originalCopy =
                Arrays.copyOf(
                        frame,
                        frame.length
                );

        fftProcessor.powerSpectrum(frame);

        assertArrayEquals(
                originalCopy,
                frame,
                TOLERANCE
        );
    }

    @Test
    void rejectsNullFrame() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> fftProcessor.powerSpectrum(null)
                );

        assertEquals(
                "Frame must not be null.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsIncorrectFrameLength() {
        float[] frame =
                new float[
                        DspConstants.WINDOW_SIZE - 1
                        ];

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> fftProcessor.powerSpectrum(
                                frame
                        )
                );

        assertEquals(
                "Expected "
                        + DspConstants.WINDOW_SIZE
                        + " samples.",
                exception.getMessage()
        );
    }

    private static float[] createTestFrame() {
        float[] frame =
                new float[DspConstants.WINDOW_SIZE];

        for (int index = 0;
             index < frame.length;
             index++) {

            frame[index] =
                    (float) (
                            0.65
                                    * Math.sin(
                                    index * 0.071
                            )
                                    + 0.25
                                    * Math.cos(
                                    index * 0.143
                            )
                    );
        }

        return frame;
    }
}