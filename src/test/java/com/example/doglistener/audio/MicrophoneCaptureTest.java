package com.example.doglistener.audio;

import com.example.doglistener.config.AudioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertFalse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MicrophoneCaptureTest {

    private static final int STANDARD_CHUNK_BYTES =
            32_000;

    @Mock
    private AudioLineProvider lineProvider;

    @Mock
    private TargetDataLine microphone;

    private AudioProperties config;

    private MicrophoneCapture capture;

    @BeforeEach
    void setUp() {
        config = new AudioProperties();

        config.setSampleRate(16_000.0f);
        config.setChannels(1);
        config.setSampleSize(16);
        config.setSigned(true);
        config.setBigEndian(false);
        config.setChunkMillis(1_000);
        config.setBufferSize(32_768);

        capture =
                new MicrophoneCapture(
                        config,
                        lineProvider
                );
    }

    @Test
    void startOpensAndStartsConfiguredLine()
            throws Exception {

        configureSupportedMicrophone();

        capture.start();

        verify(lineProvider)
                .isLineSupported(
                        any(DataLine.Info.class)
                );

        verify(lineProvider)
                .getTargetDataLine(
                        any(DataLine.Info.class)
                );

        verify(microphone)
                .open(
                        any(AudioFormat.class),
                        eq(32_768)
                );

        verify(microphone).start();
    }

    @Test
    void startUsesExpectedAudioFormat()
            throws Exception {

        configureSupportedMicrophone();

        capture.start();

        var formatCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        AudioFormat.class
                );

        verify(microphone).open(
                formatCaptor.capture(),
                eq(32_768)
        );

        AudioFormat format =
                formatCaptor.getValue();

        assertEquals(
                16_000.0f,
                format.getSampleRate()
        );

        assertEquals(
                16,
                format.getSampleSizeInBits()
        );

        assertEquals(
                1,
                format.getChannels()
        );

        assertEquals(
                AudioFormat.Encoding.PCM_SIGNED,
                format.getEncoding()
        );

        assertFalse(
                format.isBigEndian()
        );
    }

    @Test
    void readsCompleteChunkAcrossPartialReads()
            throws Exception {

        configureSupportedMicrophone();

        capture.start();

        doAnswer(invocation -> {
            byte[] buffer =
                    invocation.getArgument(0);

            int offset =
                    invocation.getArgument(1);

            int requestedLength =
                    invocation.getArgument(2);

            int bytesToReturn =
                    offset == 0
                            ? 12_000
                            : requestedLength;

            byte value =
                    offset == 0
                            ? (byte) 0x11
                            : (byte) 0x22;

            Arrays.fill(
                    buffer,
                    offset,
                    offset + bytesToReturn,
                    value
            );

            return bytesToReturn;

        }).when(microphone).read(
                any(byte[].class),
                anyInt(),
                anyInt()
        );

        AudioChunk chunk =
                capture.readChunk();

        assertEquals(
                STANDARD_CHUNK_BYTES,
                chunk.getPcm().length
        );

        for (int index = 0;
             index < 12_000;
             index++) {

            assertEquals(
                    (byte) 0x11,
                    chunk.getPcm()[index]
            );
        }

        for (int index = 12_000;
             index < STANDARD_CHUNK_BYTES;
             index++) {

            assertEquals(
                    (byte) 0x22,
                    chunk.getPcm()[index]
            );
        }

        assertTrue(
                chunk.getTimestamp() > 0
        );

        verify(microphone, times(2))
                .read(
                        any(byte[].class),
                        anyInt(),
                        anyInt()
                );
    }

    @Test
    void calculatesChunkSizeFromConfiguration()
            throws Exception {

        config.setSampleRate(8_000.0f);
        config.setChunkMillis(250);
        config.setChannels(2);
        config.setSampleSize(16);

        configureSupportedMicrophone();

        capture.start();

        when(
                microphone.read(
                        any(byte[].class),
                        anyInt(),
                        anyInt()
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(2)
        );

        AudioChunk chunk =
                capture.readChunk();

        /*
         * 8,000 samples/second
         * × 0.25 seconds
         * × 2 channels
         * × 2 bytes/sample
         * = 8,000 bytes
         */
        assertEquals(
                8_000,
                chunk.getPcm().length
        );
    }

    @Test
    void rejectsReadBeforeStart() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        capture::readChunk
                );

        assertEquals(
                "Microphone is not started.",
                exception.getMessage()
        );

        verifyNoInteractions(microphone);
    }

    @Test
    void rejectsZeroByteRead()
            throws Exception {

        configureSupportedMicrophone();

        capture.start();

        when(
                microphone.read(
                        any(byte[].class),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(0);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        capture::readChunk
                );

        assertEquals(
                "Microphone stopped before "
                        + "the audio chunk was complete.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsEndOfStreamRead()
            throws Exception {

        configureSupportedMicrophone();

        capture.start();

        when(
                microphone.read(
                        any(byte[].class),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(-1);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        capture::readChunk
                );

        assertEquals(
                "Microphone stopped before "
                        + "the audio chunk was complete.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsUnsupportedMicrophoneFormat() throws Exception {
        when(
                lineProvider.isLineSupported(
                        any(DataLine.Info.class)
                )
        ).thenReturn(false);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        capture::start
                );

        assertEquals(
                "Microphone format not supported.",
                exception.getMessage()
        );

        verify(lineProvider, never())
                .getTargetDataLine(
                        any(DataLine.Info.class)
                );

        verifyNoInteractions(microphone);
    }

    @Test
    void rejectsSecondStart()
            throws Exception {

        configureSupportedMicrophone();

        capture.start();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        capture::start
                );

        assertEquals(
                "Microphone is already started.",
                exception.getMessage()
        );

        verify(lineProvider, times(1))
                .getTargetDataLine(
                        any(DataLine.Info.class)
                );
    }

    @Test
    void closesLineWhenOpeningFails()
            throws Exception {

        configureSupportedMicrophone();

        LineUnavailableException failure =
                new LineUnavailableException(
                        "Simulated open failure"
                );

        doThrow(failure)
                .when(microphone)
                .open(
                        any(AudioFormat.class),
                        eq(32_768)
                );

        LineUnavailableException thrown =
                assertThrows(
                        LineUnavailableException.class,
                        capture::start
                );

        assertSame(failure, thrown);

        verify(microphone).close();
        verify(microphone, never()).start();
    }

    @Test
    void stopStopsClosesAndClearsMicrophone()
            throws Exception {

        configureSupportedMicrophone();

        capture.start();

        capture.stop();

        InOrder lifecycleOrder =
                inOrder(microphone);

        lifecycleOrder.verify(microphone)
                .stop();

        lifecycleOrder.verify(microphone)
                .close();

        assertThrows(
                IllegalStateException.class,
                capture::readChunk
        );

        /*
         * A second stop must be harmless and must not
         * stop or close the old line again.
         */
        capture.stop();

        verify(microphone, times(1))
                .stop();

        verify(microphone, times(1))
                .close();
    }

    @Test
    void stopBeforeStartIsHarmless() {
        capture.stop();

        verifyNoInteractions(microphone);
        verifyNoInteractions(lineProvider);
    }

    @Test
    void rejectsSampleSizeThatIsNotWholeBytes() throws Exception {
        config.setSampleSize(12);

        when(
                lineProvider.isLineSupported(
                        any(DataLine.Info.class)
                )
        ).thenReturn(true);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        capture::start
                );

        assertEquals(
                "Audio sample size must be "
                        + "a positive multiple of 8.",
                exception.getMessage()
        );

        verify(lineProvider, never())
                .getTargetDataLine(
                        any(DataLine.Info.class)
                );
    }

    private void configureSupportedMicrophone()
            throws Exception {

        when(
                lineProvider.isLineSupported(
                        any(DataLine.Info.class)
                )
        ).thenReturn(true);

        when(
                lineProvider.getTargetDataLine(
                        any(DataLine.Info.class)
                )
        ).thenReturn(microphone);
    }
}