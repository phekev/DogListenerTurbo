package com.example.doglistener.audio;

import com.example.doglistener.config.AudioProperties;

import javax.sound.sampled.AudioFormat;

public final class AudioFormatFactory {

    private AudioFormatFactory() {
    }

    public static AudioFormat create(
            AudioProperties config
    ) {
        if (config == null) {
            throw new IllegalArgumentException(
                    "Audio configuration must not be null."
            );
        }

        return new AudioFormat(
                config.getSampleRate(),
                config.getSampleSize(),
                config.getChannels(),
                config.isSigned(),
                config.isBigEndian()
        );
    }
}