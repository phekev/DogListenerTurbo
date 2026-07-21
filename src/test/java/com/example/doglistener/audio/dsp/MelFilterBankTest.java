package com.example.doglistener.audio.dsp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MelFilterBankTest {

    private static final float TOLERANCE =
            0.000001f;

    private MelFilterBank filterBank;

    @BeforeEach
    void setUp() {
        filterBank = new MelFilterBank();
    }

    @Test
    void createsExpectedNumberOfMelBins() {
        float[] powerSpectrum =
                new float[DspConstants.FFT_BINS];

        float[] result =
                filterBank.apply(powerSpectrum);

        assertEquals(
                DspConstants.MEL_BINS,
                result.length
        );
    }

    @Test
    void silenceProducesLogOffsetForEveryMelBin() {
        float[] powerSpectrum =
                new float[DspConstants.FFT_BINS];

        float[] result =
                filterBank.apply(powerSpectrum);

        float expected =
                (float) Math.log(
                        DspConstants.LOG_OFFSET
                );

        for (float value : result) {
            assertEquals(
                    expected,
                    value,
                    TOLERANCE
            );
        }
    }

    @Test
    void positiveSpectrumProducesFiniteValues() {
        float[] powerSpectrum =
                new float[DspConstants.FFT_BINS];

        for (int index = 0;
             index < powerSpectrum.length;
             index++) {

            powerSpectrum[index] =
                    0.01f + index * 0.001f;
        }

        float[] result =
                filterBank.apply(powerSpectrum);

        for (float value : result) {
            assertTrue(
                    Float.isFinite(value),
                    "Mel filter bank produced "
                            + "a non-finite value: "
                            + value
            );
        }
    }

    @Test
    void positiveSpectrumProducesMoreEnergyThanSilence() {
        float[] silence =
                new float[DspConstants.FFT_BINS];

        float[] positiveSpectrum =
                new float[DspConstants.FFT_BINS];

        Arrays.fill(
                positiveSpectrum,
                1.0f
        );

        float[] silenceResult =
                filterBank.apply(silence);

        float[] positiveResult =
                filterBank.apply(
                        positiveSpectrum
                );

        boolean foundGreaterValue = false;

        for (int index = 0;
             index < positiveResult.length;
             index++) {

            assertTrue(
                    positiveResult[index]
                            >= silenceResult[index]
            );

            if (positiveResult[index]
                    > silenceResult[index]
                    + TOLERANCE) {

                foundGreaterValue = true;
            }
        }

        assertTrue(
                foundGreaterValue,
                "A positive spectrum did not increase "
                        + "any Mel-bin energy."
        );
    }

    @Test
    void producesDeterministicOutput() {
        float[] powerSpectrum =
                createTestSpectrum();

        float[] firstResult =
                filterBank.apply(powerSpectrum);

        float[] secondResult =
                filterBank.apply(powerSpectrum);

        assertArrayEquals(
                firstResult,
                secondResult,
                TOLERANCE
        );
    }

    @Test
    void differentFrequencyBinsProduceDifferentMelOutputs() {
        float[] lowFrequencySpectrum =
                new float[DspConstants.FFT_BINS];

        float[] highFrequencySpectrum =
                new float[DspConstants.FFT_BINS];

        lowFrequencySpectrum[10] = 1.0f;

        highFrequencySpectrum[
                DspConstants.FFT_BINS - 20
                ] = 1.0f;

        float[] lowFrequencyResult =
                filterBank.apply(
                        lowFrequencySpectrum
                );

        float[] highFrequencyResult =
                filterBank.apply(
                        highFrequencySpectrum
                );

        assertFalse(
                arraysAreEqual(
                        lowFrequencyResult,
                        highFrequencyResult
                ),
                "Low-frequency and high-frequency "
                        + "energy produced identical "
                        + "Mel outputs."
        );
    }

    @Test
    void doesNotModifyInputSpectrum() {
        float[] powerSpectrum =
                createTestSpectrum();

        float[] originalCopy =
                Arrays.copyOf(
                        powerSpectrum,
                        powerSpectrum.length
                );

        filterBank.apply(powerSpectrum);

        assertArrayEquals(
                originalCopy,
                powerSpectrum
        );
    }

    @Test
    void rejectsNullPowerSpectrum() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> filterBank.apply(null)
                );

        assertEquals(
                "Power spectrum must not be null.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsIncorrectFftBinCount() {
        float[] powerSpectrum =
                new float[
                        DspConstants.FFT_BINS - 1
                        ];

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> filterBank.apply(
                                powerSpectrum
                        )
                );

        assertEquals(
                "Expected "
                        + DspConstants.FFT_BINS
                        + " FFT bins.",
                exception.getMessage()
        );
    }

    private static float[] createTestSpectrum() {
        float[] spectrum =
                new float[DspConstants.FFT_BINS];

        for (int index = 0;
             index < spectrum.length;
             index++) {

            spectrum[index] =
                    0.05f
                            + (index % 17) * 0.01f;
        }

        return spectrum;
    }

    private static boolean arraysAreEqual(
            float[] first,
            float[] second
    ) {
        if (first.length != second.length) {
            return false;
        }

        for (int index = 0;
             index < first.length;
             index++) {

            if (Math.abs(
                    first[index]
                            - second[index]
            ) > TOLERANCE) {

                return false;
            }
        }

        return true;
    }
}