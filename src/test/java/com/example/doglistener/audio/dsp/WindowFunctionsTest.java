package com.example.doglistener.audio.dsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowFunctionsTest {

    private static final float TOLERANCE =
            0.000001f;

    @Test
    void createsRequestedWindowSize() {
        float[] window =
                WindowFunctions.hann(512);

        assertEquals(
                512,
                window.length
        );
    }

    @Test
    void hannWindowStartsAndEndsAtZero() {
        float[] window =
                WindowFunctions.hann(512);

        assertEquals(
                0.0f,
                window[0],
                TOLERANCE
        );

        assertEquals(
                0.0f,
                window[window.length - 1],
                TOLERANCE
        );
    }

    @Test
    void hannWindowIsSymmetrical() {
        float[] window =
                WindowFunctions.hann(512);

        for (int index = 0;
             index < window.length;
             index++) {

            assertEquals(
                    window[index],
                    window[
                            window.length - 1 - index
                            ],
                    TOLERANCE
            );
        }
    }

    @Test
    void hannCoefficientsRemainBetweenZeroAndOne() {
        float[] window =
                WindowFunctions.hann(512);

        for (float coefficient : window) {
            assertTrue(
                    coefficient >= 0.0f,
                    "Coefficient was below zero: "
                            + coefficient
            );

            assertTrue(
                    coefficient <= 1.0f,
                    "Coefficient exceeded one: "
                            + coefficient
            );
        }
    }

    @Test
    void oddSizedHannWindowHasCenterValueOfOne() {
        float[] window =
                WindowFunctions.hann(5);

        assertEquals(
                1.0f,
                window[2],
                TOLERANCE
        );
    }

    @Test
    void createsExpectedSmallHannWindow() {
        float[] window =
                WindowFunctions.hann(5);

        assertArrayEquals(
                new float[] {
                        0.0f,
                        0.5f,
                        1.0f,
                        0.5f,
                        0.0f
                },
                window,
                TOLERANCE
        );
    }

    @Test
    void rejectsWindowSizeBelowTwo() {
        IllegalArgumentException zeroException =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WindowFunctions.hann(0)
                );

        IllegalArgumentException oneException =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WindowFunctions.hann(1)
                );

        assertEquals(
                "Window size must be at least 2.",
                zeroException.getMessage()
        );

        assertEquals(
                "Window size must be at least 2.",
                oneException.getMessage()
        );
    }

    @Test
    void appliesWindowToFrameInPlace() {
        float[] frame = {
                2.0f,
                4.0f,
                6.0f,
                8.0f
        };

        float[] window = {
                0.0f,
                0.25f,
                0.50f,
                1.0f
        };

        WindowFunctions.apply(
                frame,
                window
        );

        assertArrayEquals(
                new float[] {
                        0.0f,
                        1.0f,
                        3.0f,
                        8.0f
                },
                frame,
                TOLERANCE
        );
    }

    @Test
    void doesNotModifyWindowWhileApplying() {
        float[] frame = {
                1.0f,
                1.0f,
                1.0f
        };

        float[] window = {
                0.0f,
                0.5f,
                1.0f
        };

        float[] originalWindow = {
                0.0f,
                0.5f,
                1.0f
        };

        WindowFunctions.apply(
                frame,
                window
        );

        assertArrayEquals(
                originalWindow,
                window,
                TOLERANCE
        );
    }

    @Test
    void rejectsDifferentFrameAndWindowSizes() {
        float[] frame =
                new float[10];

        float[] window =
                new float[9];

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WindowFunctions.apply(
                                frame,
                                window
                        )
                );

        assertEquals(
                "Frame and window sizes differ.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsNullFrameOrWindow() {
        IllegalArgumentException frameException =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WindowFunctions.apply(
                                null,
                                new float[4]
                        )
                );

        IllegalArgumentException windowException =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WindowFunctions.apply(
                                new float[4],
                                null
                        )
                );

        assertEquals(
                "Frame must not be null.",
                frameException.getMessage()
        );

        assertEquals(
                "Window must not be null.",
                windowException.getMessage()
        );
    }
}