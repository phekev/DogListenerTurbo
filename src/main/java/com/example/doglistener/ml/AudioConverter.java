package com.example.doglistener.ml;

public final class AudioConverter {

    private static final float PCM_16_SCALE =
            32768.0f;

    private AudioConverter() {
    }

    public static float[] pcm16ToFloat(
            byte[] pcm
    ) {
        if (pcm == null) {
            throw new IllegalArgumentException(
                    "PCM data must not be null."
            );
        }

        if (pcm.length == 0) {
            throw new IllegalArgumentException(
                    "PCM data must not be empty."
            );
        }

        if (pcm.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "PCM data must contain complete "
                            + "16-bit samples."
            );
        }

        int sampleCount =
                pcm.length / 2;

        float[] output =
                new float[sampleCount];

        for (int sampleIndex = 0;
             sampleIndex < sampleCount;
             sampleIndex++) {

            int byteIndex =
                    sampleIndex * 2;

            int lowByte =
                    pcm[byteIndex] & 0xff;

            int highByte =
                    pcm[byteIndex + 1];

            short sample =
                    (short) (
                            (highByte << 8)
                                    | lowByte
                    );

            output[sampleIndex] =
                    sample / PCM_16_SCALE;
        }

        return output;
    }
}