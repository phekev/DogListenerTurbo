package com.example.doglistener.ml;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioConverterTest {

    private static final float TOLERANCE =
            0.000001f;

    @Test
    void convertsZeroSample() {
        byte[] pcm = {
                0x00,
                0x00
        };

        float[] result =
                AudioConverter.pcm16ToFloat(pcm);

        assertArrayEquals(
                new float[] {
                        0.0f
                },
                result,
                TOLERANCE
        );
    }

    @Test
    void convertsMaximumPositiveSample() {
        byte[] pcm = {
                (byte) 0xff,
                0x7f
        };

        float[] result =
                AudioConverter.pcm16ToFloat(pcm);

        assertEquals(
                32767.0f / 32768.0f,
                result[0],
                TOLERANCE
        );
    }

    @Test
    void convertsMinimumNegativeSample() {
        byte[] pcm = {
                0x00,
                (byte) 0x80
        };

        float[] result =
                AudioConverter.pcm16ToFloat(pcm);

        assertEquals(
                -1.0f,
                result[0],
                TOLERANCE
        );
    }

    @Test
    void convertsNegativeOneSample() {
        byte[] pcm = {
                (byte) 0xff,
                (byte) 0xff
        };

        float[] result =
                AudioConverter.pcm16ToFloat(pcm);

        assertEquals(
                -1.0f / 32768.0f,
                result[0],
                TOLERANCE
        );
    }

    @Test
    void interpretsBytesAsLittleEndian() {
        byte[] pcm = {
                0x34,
                0x12
        };

        float[] result =
                AudioConverter.pcm16ToFloat(pcm);

        assertEquals(
                0x1234 / 32768.0f,
                result[0],
                TOLERANCE
        );
    }

    @Test
    void convertsMultipleSamplesInOrder() {
        byte[] pcm = {
                0x00, 0x00,
                0x00, 0x40,
                0x00, (byte) 0xc0,
                (byte) 0xff, 0x7f,
                0x00, (byte) 0x80
        };

        float[] result =
                AudioConverter.pcm16ToFloat(pcm);

        assertArrayEquals(
                new float[] {
                        0.0f,
                        0.5f,
                        -0.5f,
                        32767.0f / 32768.0f,
                        -1.0f
                },
                result,
                TOLERANCE
        );
    }

    @Test
    void createsOneFloatPerTwoPcmBytes() {
        byte[] pcm =
                new byte[32_000];

        float[] result =
                AudioConverter.pcm16ToFloat(pcm);

        assertEquals(
                16_000,
                result.length
        );
    }

    @Test
    void doesNotModifyPcmInput() {
        byte[] pcm = {
                0x34,
                0x12,
                (byte) 0xff,
                (byte) 0xff
        };

        byte[] originalCopy =
                Arrays.copyOf(
                        pcm,
                        pcm.length
                );

        AudioConverter.pcm16ToFloat(pcm);

        assertArrayEquals(
                originalCopy,
                pcm
        );
    }

    @Test
    void rejectsNullPcmData() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> AudioConverter
                                .pcm16ToFloat(null)
                );

        assertEquals(
                "PCM data must not be null.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsEmptyPcmData() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> AudioConverter
                                .pcm16ToFloat(
                                        new byte[0]
                                )
                );

        assertEquals(
                "PCM data must not be empty.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsIncompletePcmSample() {
        byte[] pcm = {
                0x00,
                0x01,
                0x02
        };

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> AudioConverter
                                .pcm16ToFloat(pcm)
                );

        assertEquals(
                "PCM data must contain complete "
                        + "16-bit samples.",
                exception.getMessage()
        );
    }
}