package com.example.doglistener.sound;

import com.example.doglistener.config.ResponseProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaSoundResponseSoundPlayerTest {

    @TempDir
    Path temporaryDirectory;

    @Mock
    private ResponseProperties responseProperties;

    @Mock
    private JavaSoundFacade javaSoundFacade;

    @Mock
    private AudioInputStream audioInputStream;

    @Mock
    private Clip clip;

    @Mock
    private FloatControl gainControl;

    private JavaSoundResponseSoundPlayer player;

    @BeforeEach
    void setUp() {
        player =
                new JavaSoundResponseSoundPlayer(
                        responseProperties,
                        javaSoundFacade
                );
    }

    @Test
    void playsExternalWaveFile()
            throws Exception {

        Path soundFile =
                createSoundFile("hey.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(1.0f);

        when(
                javaSoundFacade.openAudioInputStream(
                        soundFile
                )
        ).thenReturn(audioInputStream);

        when(
                javaSoundFacade.createClip()
        ).thenReturn(clip);

        when(
                clip.isControlSupported(
                        FloatControl.Type.MASTER_GAIN
                )
        ).thenReturn(false);

        player.play(soundFile);

        verify(clip).open(audioInputStream);
        verify(clip).setFramePosition(0);
        verify(clip).start();
        verify(audioInputStream).close();
    }

    @Test
    void appliesConfiguredVolume()
            throws Exception {

        Path soundFile =
                createSoundFile("stop.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(0.5f);

        when(
                javaSoundFacade.openAudioInputStream(
                        soundFile
                )
        ).thenReturn(audioInputStream);

        when(
                javaSoundFacade.createClip()
        ).thenReturn(clip);

        when(
                clip.isControlSupported(
                        FloatControl.Type.MASTER_GAIN
                )
        ).thenReturn(true);

        when(
                clip.getControl(
                        FloatControl.Type.MASTER_GAIN
                )
        ).thenReturn(gainControl);

        when(
                gainControl.getMinimum()
        ).thenReturn(-80.0f);

        when(
                gainControl.getMaximum()
        ).thenReturn(6.0f);

        player.play(soundFile);

        ArgumentCaptor<Float> gainCaptor =
                ArgumentCaptor.forClass(
                        Float.class
                );

        verify(gainControl).setValue(
                gainCaptor.capture()
        );

        assertEquals(
                -6.0206f,
                gainCaptor.getValue(),
                0.001f
        );
    }

    @Test
    void zeroVolumeUsesMinimumGain()
            throws Exception {

        Path soundFile =
                createSoundFile("quiet.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(0.0f);

        when(
                javaSoundFacade.openAudioInputStream(
                        soundFile
                )
        ).thenReturn(audioInputStream);

        when(
                javaSoundFacade.createClip()
        ).thenReturn(clip);

        when(
                clip.isControlSupported(
                        FloatControl.Type.MASTER_GAIN
                )
        ).thenReturn(true);

        when(
                clip.getControl(
                        FloatControl.Type.MASTER_GAIN
                )
        ).thenReturn(gainControl);

        when(
                gainControl.getMinimum()
        ).thenReturn(-80.0f);

        when(
                gainControl.getMaximum()
        ).thenReturn(6.0f);

        player.play(soundFile);

        verify(gainControl).setValue(
                -80.0f
        );
    }

    @Test
    void replacesCurrentlyPlayingClip()
            throws Exception {

        Path firstSound =
                createSoundFile("hey.wav");

        Path secondSound =
                createSoundFile("stop.wav");

        AudioInputStream secondInputStream =
                org.mockito.Mockito.mock(
                        AudioInputStream.class
                );

        Clip secondClip =
                org.mockito.Mockito.mock(
                        Clip.class
                );

        when(
                responseProperties.getVolume()
        ).thenReturn(1.0f);

        when(
                javaSoundFacade.openAudioInputStream(
                        firstSound
                )
        ).thenReturn(audioInputStream);

        when(
                javaSoundFacade.openAudioInputStream(
                        secondSound
                )
        ).thenReturn(secondInputStream);

        when(
                javaSoundFacade.createClip()
        ).thenReturn(
                clip,
                secondClip
        );

        player.play(firstSound);
        player.play(secondSound);

        verify(clip).close();
        verify(secondClip).start();
    }

    @Test
    void closesActiveClipOnShutdown()
            throws Exception {

        Path soundFile =
                createSoundFile("enough.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(1.0f);

        when(
                javaSoundFacade.openAudioInputStream(
                        soundFile
                )
        ).thenReturn(audioInputStream);

        when(
                javaSoundFacade.createClip()
        ).thenReturn(clip);

        player.play(soundFile);
        player.stop();

        verify(clip).close();
    }

    @Test
    void closesClipWhenPlaybackStops()
            throws Exception {

        Path soundFile =
                createSoundFile("now.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(1.0f);

        when(
                javaSoundFacade.openAudioInputStream(
                        soundFile
                )
        ).thenReturn(audioInputStream);

        when(
                javaSoundFacade.createClip()
        ).thenReturn(clip);

        player.play(soundFile);

        ArgumentCaptor<LineListener> listenerCaptor =
                ArgumentCaptor.forClass(
                        LineListener.class
                );

        verify(clip).addLineListener(
                listenerCaptor.capture()
        );

        listenerCaptor
                .getValue()
                .update(
                        new javax.sound.sampled.LineEvent(
                                clip,
                                javax.sound.sampled.LineEvent
                                        .Type.STOP,
                                0L
                        )
                );

        verify(clip).close();
    }

    @Test
    void rejectsMissingSoundFile() {
        Path missingFile =
                temporaryDirectory
                        .resolve("missing.wav")
                        .toAbsolutePath()
                        .normalize();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> player.play(missingFile)
                );

        assertEquals(
                "Response sound file does not exist: "
                        + missingFile,
                exception.getMessage()
        );

        verifyNoInteractions(
                responseProperties,
                javaSoundFacade
        );
    }

    @Test
    void rejectsNonWaveFile()
            throws Exception {

        Path soundFile =
                temporaryDirectory
                        .resolve("response.mp3")
                        .toAbsolutePath()
                        .normalize();

        Files.write(
                soundFile,
                new byte[] {0x00}
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> player.play(soundFile)
        );

        verifyNoInteractions(
                responseProperties,
                javaSoundFacade
        );
    }

    @Test
    void rejectsInvalidVolume()
            throws Exception {

        Path soundFile =
                createSoundFile("stop.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(1.5f);

        assertThrows(
                IllegalArgumentException.class,
                () -> player.play(soundFile)
        );

        verifyNoInteractions(
                javaSoundFacade
        );
    }

    @Test
    void wrapsUnsupportedAudioFailure()
            throws Exception {

        Path soundFile =
                createSoundFile("invalid.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(1.0f);

        when(
                javaSoundFacade.openAudioInputStream(
                        soundFile
                )
        ).thenThrow(
                new UnsupportedAudioFileException(
                        "Invalid WAV"
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> player.play(soundFile)
        );

        verify(
                javaSoundFacade,
                never()
        ).createClip();
    }

    @Test
    void closesClipWhenOpeningFails()
            throws Exception {

        Path soundFile =
                createSoundFile("broken.wav");

        when(
                responseProperties.getVolume()
        ).thenReturn(1.0f);

        when(
                javaSoundFacade.openAudioInputStream(
                        soundFile
                )
        ).thenReturn(audioInputStream);

        when(
                javaSoundFacade.createClip()
        ).thenReturn(clip);

        doThrow(
                new java.io.IOException(
                        "Simulated opening failure"
                )
        ).when(clip)
                .open(audioInputStream);

        assertThrows(
                IllegalStateException.class,
                () -> player.play(soundFile)
        );

        verify(clip).close();
    }

    private Path createSoundFile(
            String filename
    ) throws Exception {

        Path soundFile =
                temporaryDirectory
                        .resolve(filename)
                        .toAbsolutePath()
                        .normalize();

        Files.write(
                soundFile,
                new byte[] {0x00}
        );

        return soundFile;
    }
}