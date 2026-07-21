
package com.example.doglistener.audio;

import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

@Component
public class AudioLineProvider {

    public boolean isLineSupported(
            DataLine.Info lineInfo
    ) {
        return AudioSystem.isLineSupported(
                lineInfo
        );
    }

    public TargetDataLine getTargetDataLine(
            DataLine.Info lineInfo
    ) throws LineUnavailableException {

        return (TargetDataLine)
                AudioSystem.getLine(lineInfo);
    }
}