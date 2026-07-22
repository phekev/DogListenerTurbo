package com.example.doglistener.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioChunkTest {

    @Test
    void storesPcmDataAndTimestamp() {
        byte[] pcm = {
                0x01,
                0x02,
                0x03,
                0x04
        };

        long timestamp =
                123_456_789L;

        AudioChunk chunk =
                new AudioChunk(
                        pcm,
                        timestamp
                );

        assertSame(
                pcm,
                chunk.getPcm()
        );

        assertEquals(
                timestamp,
                chunk.getTimestamp()
        );
    }

    @Test
    void retainsPcmArrayWithoutCopying() {
        byte[] pcm =
                new byte[32_000];

        AudioChunk chunk =
                new AudioChunk(
                        pcm,
                        100L
                );

        pcm[50] = 0x55;

        assertSame(
                pcm,
                chunk.getPcm()
        );

        assertEquals(
                (byte) 0x55,
                chunk.getPcm()[50]
        );
    }

    @Test
    void acceptsNegativeNanoTimeValue() {
        AudioChunk chunk =
                new AudioChunk(
                        new byte[] {
                                0x00,
                                0x00
                        },
                        -100L
                );

        assertEquals(
                -100L,
                chunk.getTimestamp()
        );
    }

    @Test
    void rejectsNullPcmData() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new AudioChunk(
                                null,
                                100L
                        )
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
                        () -> new AudioChunk(
                                new byte[0],
                                100L
                        )
                );

        assertEquals(
                "PCM data must not be empty.",
                exception.getMessage()
        );
    }
}