package com.example.doglistener.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnnxScoreExtractorTest {

    private static final float TOLERANCE =
            0.0001f;

    private OnnxScoreExtractor scoreExtractor;

    @BeforeEach
    void setUp() {
        scoreExtractor =
                new OnnxScoreExtractor();
    }

    @Test
    void returnsOneDimensionalScores() {
        float[] scores = {
                0.10f,
                0.20f,
                0.30f
        };

        float[] result =
                scoreExtractor.extract(scores);

        assertSame(scores, result);
    }

    @Test
    void returnsSingleTwoDimensionalRow() {
        float[] row = {
                0.10f,
                0.20f,
                0.30f
        };

        float[][] scores = {
                row
        };

        float[] result =
                scoreExtractor.extract(scores);

        assertSame(row, result);
    }

    @Test
    void averagesTwoDimensionalRows() {
        float[][] scores = {
                {
                        1.0f,
                        3.0f,
                        5.0f
                },
                {
                        3.0f,
                        5.0f,
                        7.0f
                }
        };

        float[] result =
                scoreExtractor.extract(scores);

        assertArrayEquals(
                new float[] {
                        2.0f,
                        4.0f,
                        6.0f
                },
                result,
                TOLERANCE
        );
    }

    @Test
    void averagesAllThreeDimensionalRows() {
        float[][][] scores = {
                {
                        {
                                1.0f,
                                3.0f
                        },
                        {
                                3.0f,
                                5.0f
                        }
                },
                {
                        {
                                5.0f,
                                7.0f
                        }
                }
        };

        float[] result =
                scoreExtractor.extract(scores);

        assertArrayEquals(
                new float[] {
                        3.0f,
                        5.0f
                },
                result,
                TOLERANCE
        );
    }

    @Test
    void rejectsNullOutput() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> scoreExtractor.extract(null)
                );

        assertTrue(
                exception.getMessage()
                        .contains("null score tensor")
        );
    }

    @Test
    void rejectsEmptyOneDimensionalOutput() {
        assertThrows(
                IllegalStateException.class,
                () -> scoreExtractor.extract(
                        new float[0]
                )
        );
    }

    @Test
    void rejectsEmptyTwoDimensionalOutput() {
        assertThrows(
                IllegalStateException.class,
                () -> scoreExtractor.extract(
                        new float[0][]
                )
        );
    }

    @Test
    void rejectsEmptyThreeDimensionalOutput() {
        assertThrows(
                IllegalStateException.class,
                () -> scoreExtractor.extract(
                        new float[0][][]
                )
        );
    }

    @Test
    void rejectsThreeDimensionalOutputWithNoRows() {
        float[][][] scores = {
                new float[0][]
        };

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> scoreExtractor.extract(scores)
                );

        assertTrue(
                exception.getMessage()
                        .contains("no score rows")
        );
    }

    @Test
    void rejectsInconsistentTwoDimensionalRows() {
        float[][] scores = {
                {
                        1.0f,
                        2.0f
                },
                {
                        3.0f
                }
        };

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> scoreExtractor.extract(scores)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Inconsistent")
        );
    }

    @Test
    void rejectsInconsistentThreeDimensionalRows() {
        float[][][] scores = {
                {
                        {
                                1.0f,
                                2.0f
                        }
                },
                {
                        {
                                3.0f
                        }
                }
        };

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> scoreExtractor.extract(scores)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Inconsistent")
        );
    }

    @Test
    void rejectsNullScoreRow() {
        float[][] scores = {
                {
                        1.0f,
                        2.0f
                },
                null
        };

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> scoreExtractor.extract(scores)
                );

        assertTrue(
                exception.getMessage()
                        .contains("null score row")
        );
    }

    @Test
    void rejectsUnsupportedOutputType() {
        int[] unsupportedScores = {
                1,
                2,
                3
        };

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> scoreExtractor.extract(
                                unsupportedScores
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Unsupported model output type"
                        )
        );
    }
}