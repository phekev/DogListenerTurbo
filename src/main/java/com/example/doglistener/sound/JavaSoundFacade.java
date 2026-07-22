package com.example.doglistener.sound;

import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Path;

@Component
public class JavaSoundFacade {

    public AudioInputStream openAudioInputStream(
            Path soundFile
    ) throws UnsupportedAudioFileException, IOException {

        return AudioSystem.getAudioInputStream(
                soundFile.toFile()
        );
    }

    public Clip createClip()
            throws LineUnavailableException {

        return AudioSystem.getClip();
    }
}