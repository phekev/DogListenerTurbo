package com.example.doglistener.ml;

import com.example.doglistener.audio.dsp.MelSpectrogramExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YamnetInputPreprocessorTest {

    private static final int FRAME_COUNT = 96;
    private static final int MEL_BIN_COUNT = 64;

    @Mock
    private WaveformPreprocessor waveformPreprocessor;

    @Mock
    private MelSpectrogramExtractor melSpectrogramExtractor;

    private YamnetInputPreprocessor inputPreprocessor;

    @BeforeEach
    void setUp() {
        inputPreprocessor =
                new YamnetInputPreprocessor(
                        waveformPreprocessor,
                        melSpectrogramExtractor
                );
    }

    @Test
    void createsExpectedYamnetInputShape() {
        float[] originalAudio =
                new float[16_000];

        float[] preparedAudio =
                new float[15_600];

        float[][] melSpectrogram =
                createMelSpectrogram(
                        FRAME_COUNT,
                        MEL_BIN_COUNT
                );

        when(
                waveformPreprocessor.prepare(originalAudio)
        ).thenReturn(preparedAudio);

        when(
                melSpectrogramExtractor.extract(preparedAudio)
        ).thenReturn(melSpectrogram);

        float[][][][] result =
                inputPreprocessor.prepare(originalAudio);

        assertEquals(1, result.length);
        assertEquals(1, result[0].length);
        assertEquals(
                FRAME_COUNT,
                result[0][0].length
        );

        for (int frame = 0;
             frame < FRAME_COUNT;
             frame++) {

            assertEquals(
                    MEL_BIN_COUNT,
                    result[0][0][frame].length
            );
        }

        verify(waveformPreprocessor)
                .prepare(originalAudio);

        verify(melSpectrogramExtractor)
                .extract(preparedAudio);
    }

    @Test
    void copiesMelSpectrogramValuesIntoModelInput() {
        float[] originalAudio =
                new float[16_000];

        float[] preparedAudio =
                new float[15_600];

        float[][] melSpectrogram =
                createMelSpectrogram(
                        FRAME_COUNT,
                        MEL_BIN_COUNT
                );

        melSpectrogram[0][0] = 1.25f;
        melSpectrogram[40][20] = -0.75f;
        melSpectrogram[95][63] = 3.50f;

        when(
                waveformPreprocessor.prepare(originalAudio)
        ).thenReturn(preparedAudio);

        when(
                melSpectrogramExtractor.extract(preparedAudio)
        ).thenReturn(melSpectrogram);

        float[][][][] result =
                inputPreprocessor.prepare(originalAudio);

        assertEquals(
                1.25f,
                result[0][0][0][0]
        );

        assertEquals(
                -0.75f,
                result[0][0][40][20]
        );

        assertEquals(
                3.50f,
                result[0][0][95][63]
        );
    }

    @Test
    void passesPreparedWaveformToMelExtractor() {
        float[] originalAudio =
                new float[16_000];

        float[] preparedAudio =
                new float[15_600];

        float[][] melSpectrogram =
                createMelSpectrogram(
                        FRAME_COUNT,
                        MEL_BIN_COUNT
                );

        when(
                waveformPreprocessor.prepare(originalAudio)
        ).thenReturn(preparedAudio);

        when(
                melSpectrogramExtractor.extract(preparedAudio)
        ).thenReturn(melSpectrogram);

        inputPreprocessor.prepare(originalAudio);

        verify(waveformPreprocessor)
                .prepare(originalAudio);

        verify(melSpectrogramExtractor)
                .extract(preparedAudio);

        assertSame(
                preparedAudio,
                preparedAudio
        );
    }

    @Test
    void rejectsIncorrectFrameCount() {
        float[] originalAudio =
                new float[16_000];

        float[] preparedAudio =
                new float[15_600];

        float[][] invalidSpectrogram =
                createMelSpectrogram(
                        95,
                        MEL_BIN_COUNT
                );

        when(
                waveformPreprocessor.prepare(originalAudio)
        ).thenReturn(preparedAudio);

        when(
                melSpectrogramExtractor.extract(preparedAudio)
        ).thenReturn(invalidSpectrogram);

        assertThrows(
                IllegalArgumentException.class,
                () -> inputPreprocessor.prepare(
                        originalAudio
                )
        );
    }

    @Test
    void rejectsIncorrectMelBinCount() {
        float[] originalAudio =
                new float[16_000];

        float[] preparedAudio =
                new float[15_600];

        float[][] invalidSpectrogram =
                createMelSpectrogram(
                        FRAME_COUNT,
                        MEL_BIN_COUNT
                );

        invalidSpectrogram[25] =
                new float[63];

        when(
                waveformPreprocessor.prepare(originalAudio)
        ).thenReturn(preparedAudio);

        when(
                melSpectrogramExtractor.extract(preparedAudio)
        ).thenReturn(invalidSpectrogram);

        assertThrows(
                IllegalArgumentException.class,
                () -> inputPreprocessor.prepare(
                        originalAudio
                )
        );
    }

    @Test
    void rejectsNullMelFrame() {
        float[] originalAudio =
                new float[16_000];

        float[] preparedAudio =
                new float[15_600];

        float[][] invalidSpectrogram =
                createMelSpectrogram(
                        FRAME_COUNT,
                        MEL_BIN_COUNT
                );

        invalidSpectrogram[50] = null;

        when(
                waveformPreprocessor.prepare(originalAudio)
        ).thenReturn(preparedAudio);

        when(
                melSpectrogramExtractor.extract(preparedAudio)
        ).thenReturn(invalidSpectrogram);

        assertThrows(
                IllegalArgumentException.class,
                () -> inputPreprocessor.prepare(
                        originalAudio
                )
        );
    }

    private static float[][] createMelSpectrogram(
            int frameCount,
            int melBinCount
    ) {
        float[][] spectrogram =
                new float[frameCount][melBinCount];

        for (int frame = 0;
             frame < frameCount;
             frame++) {

            for (int bin = 0;
                 bin < melBinCount;
                 bin++) {

                spectrogram[frame][bin] =
                        frame * 0.01f
                                + bin * 0.001f;
            }
        }

        return spectrogram;
    }
}