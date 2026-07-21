package com.example.doglistener.audio.dsp;

/**
 * Utility methods for creating DSP window functions.
 */
public final class WindowFunctions {

    private WindowFunctions() {
    }

    /**
     * Creates a Hann window.
     *
     * @param size Number of samples in the window.
     * @return Window coefficients.
     */
    public static float[] hann(int size) {

        if (size < 2) {
            throw new IllegalArgumentException(
                    "Window size must be at least 2."
            );
        }

        float[] window = new float[size];

        for (int n = 0; n < size; n++) {

            window[n] = (float) (
                    0.5
                            - 0.5
                            * Math.cos(
                            (2.0 * Math.PI * n)
                                    / (size - 1)
                    )
            );
        }

        return window;
    }

    /**
     * Applies a window to a frame in-place.
     */
    public static void apply(
            float[] frame,
            float[] window
    ) {

        if (frame == null) {
            throw new IllegalArgumentException(
                    "Frame must not be null."
            );
        }

        if (window == null) {
            throw new IllegalArgumentException(
                    "Window must not be null."
            );
        }

        if (frame.length != window.length) {
            throw new IllegalArgumentException(
                    "Frame and window sizes differ."
            );
        }

        for (int i = 0; i < frame.length; i++) {
            frame[i] *= window[i];
        }
    }
}