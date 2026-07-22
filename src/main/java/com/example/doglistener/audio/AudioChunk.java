package com.example.doglistener.audio;

public class AudioChunk {

    private final byte[] pcm;
    private final long timestamp;

    public AudioChunk(
            byte[] pcm,
            long timestamp
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

        this.pcm = pcm;
        this.timestamp = timestamp;
    }

    public byte[] getPcm() {
        return pcm;
    }

    public long getTimestamp() {
        return timestamp;
    }
}