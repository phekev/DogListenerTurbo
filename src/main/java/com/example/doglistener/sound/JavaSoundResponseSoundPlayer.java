package com.example.doglistener.sound;

import com.example.doglistener.config.ResponseProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class JavaSoundResponseSoundPlayer
        implements ResponseSoundPlayer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    JavaSoundResponseSoundPlayer.class
            );

    private final ResponseProperties responseProperties;
    private final JavaSoundFacade javaSoundFacade;

    private final Object clipLock =
            new Object();

    private Clip activeClip;

    public JavaSoundResponseSoundPlayer(
            ResponseProperties responseProperties,
            JavaSoundFacade javaSoundFacade
    ) {
        this.responseProperties =
                responseProperties;

        this.javaSoundFacade =
                javaSoundFacade;
    }

    @Override
    public void play(Path soundFile) {
        Path validatedSoundFile =
                validateSoundFile(soundFile);

        float volume =
                validateVolume(
                        responseProperties.getVolume()
                );

        Clip newClip = null;

        try (
                AudioInputStream audioInputStream =
                        javaSoundFacade
                                .openAudioInputStream(
                                        validatedSoundFile
                                )
        ) {
            newClip =
                    javaSoundFacade.createClip();

            newClip.open(audioInputStream);

            applyVolume(
                    newClip,
                    volume
            );

            startClip(
                    newClip,
                    validatedSoundFile
            );

        } catch (
                UnsupportedAudioFileException
                | IOException
                | LineUnavailableException exception
        ) {
            releaseClip(newClip);

            throw new IllegalStateException(
                    "Unable to play response sound: "
                            + validatedSoundFile,
                    exception
            );

        } catch (RuntimeException exception) {
            releaseClip(newClip);

            throw new IllegalStateException(
                    "Unable to play response sound: "
                            + validatedSoundFile,
                    exception
            );
        }
    }

    private Path validateSoundFile(
            Path soundFile
    ) {
        if (soundFile == null) {
            throw new IllegalArgumentException(
                    "Response sound file must not be null."
            );
        }

        Path normalizedPath =
                soundFile
                        .toAbsolutePath()
                        .normalize();

        if (!Files.isRegularFile(normalizedPath)) {
            throw new IllegalArgumentException(
                    "Response sound file does not exist: "
                            + normalizedPath
            );
        }

        if (!Files.isReadable(normalizedPath)) {
            throw new IllegalArgumentException(
                    "Response sound file is not readable: "
                            + normalizedPath
            );
        }

        String filename =
                normalizedPath
                        .getFileName()
                        .toString()
                        .toLowerCase(Locale.ROOT);

        if (!filename.endsWith(".wav")) {
            throw new IllegalArgumentException(
                    "Response sound file must be a WAV file: "
                            + normalizedPath
            );
        }

        return normalizedPath;
    }

    private float validateVolume(float volume) {
        if (!Float.isFinite(volume)
                || volume < 0.0f
                || volume > 1.0f) {

            throw new IllegalArgumentException(
                    "Response volume must be between "
                            + "0.0 and 1.0."
            );
        }

        return volume;
    }

    private void applyVolume(
            Clip clip,
            float volume
    ) {
        if (!clip.isControlSupported(
                FloatControl.Type.MASTER_GAIN
        )) {
            LOGGER.debug(
                    "Audio output does not support "
                            + "MASTER_GAIN control."
            );

            return;
        }

        FloatControl gainControl =
                (FloatControl) clip.getControl(
                        FloatControl.Type.MASTER_GAIN
                );

        float requestedGain;

        if (volume == 0.0f) {
            requestedGain =
                    gainControl.getMinimum();
        } else {
            requestedGain =
                    20.0f
                            * (float) Math.log10(volume);
        }

        float clampedGain =
                Math.max(
                        gainControl.getMinimum(),
                        Math.min(
                                gainControl.getMaximum(),
                                requestedGain
                        )
                );

        gainControl.setValue(clampedGain);
    }

    private void startClip(
            Clip clip,
            Path soundFile
    ) {
        clip.addLineListener(
                event -> {
                    if (event.getType()
                            == LineEvent.Type.STOP) {

                        onClipStopped(clip);
                    }
                }
        );

        synchronized (clipLock) {
            closeActiveClipLocked();

            activeClip = clip;

            clip.setFramePosition(0);
            clip.start();
        }

        LOGGER.info(
                "Response sound started: {}",
                soundFile
        );
    }

    private void onClipStopped(Clip clip) {
        synchronized (clipLock) {
            if (activeClip == clip) {
                activeClip = null;
            }
        }

        closeClip(clip);
    }

    private void releaseClip(Clip clip) {
        if (clip == null) {
            return;
        }

        synchronized (clipLock) {
            if (activeClip == clip) {
                activeClip = null;
            }
        }

        closeClip(clip);
    }

    private void closeActiveClipLocked() {
        Clip clipToClose =
                activeClip;

        activeClip = null;

        closeClip(clipToClose);
    }

    private void closeClip(Clip clip) {
        if (clip == null) {
            return;
        }

        try {
            clip.close();
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Unable to close response audio clip.",
                    exception
            );
        }
    }

    @PreDestroy
    public void stop() {
        synchronized (clipLock) {
            closeActiveClipLocked();
        }
    }
}