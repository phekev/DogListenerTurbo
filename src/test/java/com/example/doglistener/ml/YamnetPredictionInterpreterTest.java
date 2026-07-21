package com.example.doglistener.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YamnetPredictionInterpreterTest {

    private static final float TOLERANCE = 0.0001f;

    @Mock
    private ClassMap classMap;

    private YamnetPredictionInterpreter interpreter;

    @BeforeEach
    void setUp() {
        interpreter =
                new YamnetPredictionInterpreter(classMap);
    }

    @Test
    void returnsCompositeDogBarkPrediction() {
        configureLabels(
                "Dog",
                "Bark",
                "Bow-wow",
                "Yip"
        );

        float[] logits = {
                0.0f,
                0.0f,
                0.0f,
                0.0f
        };

        Prediction prediction =
                interpreter.interpret(logits);

        assertEquals(-1, prediction.classId());
        assertEquals("Dog Bark", prediction.label());

        /*
         * sigmoid(0) = 0.5
         *
         * 0.5 × 0.5
         * + 0.3 × 0.5
         * + 0.1 × 0.5
         * + 0.1 × 0.5
         * = 0.5
         */
        assertEquals(
                0.50f,
                prediction.confidence(),
                TOLERANCE
        );
    }

    @Test
    void calculatesWeightedBarkConfidence() {
        configureLabels(
                "Dog",
                "Bark",
                "Bow-wow",
                "Yip"
        );

        float dogProbability = 0.80f;
        float barkProbability = 0.60f;
        float bowWowProbability = 0.40f;
        float yipProbability = 0.20f;

        float[] logits = {
                toLogit(dogProbability),
                toLogit(barkProbability),
                toLogit(bowWowProbability),
                toLogit(yipProbability)
        };

        Prediction prediction =
                interpreter.interpret(logits);

        float expectedConfidence =
                0.5f * dogProbability
                        + 0.3f * barkProbability
                        + 0.1f * bowWowProbability
                        + 0.1f * yipProbability;

        assertEquals(
                0.64f,
                expectedConfidence,
                TOLERANCE
        );

        assertEquals(
                expectedConfidence,
                prediction.confidence(),
                TOLERANCE
        );
    }

    @Test
    void unrelatedClassesDoNotAffectWeightedScore() {
        configureLabels(
                "Dog",
                "Bark",
                "Bow-wow",
                "Yip",
                "Speech",
                "Music"
        );

        float[] logits = {
                toLogit(0.20f),
                toLogit(0.20f),
                toLogit(0.20f),
                toLogit(0.20f),
                toLogit(0.99f),
                toLogit(0.98f)
        };

        Prediction prediction =
                interpreter.interpret(logits);

        /*
         * Each bark component is 0.20.
         * The weights total 1.0, so the result is 0.20.
         *
         * Speech and Music must not affect the composite score.
         */
        assertEquals(
                0.20f,
                prediction.confidence(),
                TOLERANCE
        );
    }

    @Test
    void missingBarkClassesContributeZero() {
        configureLabels(
                "Dog",
                "Speech",
                "Music"
        );

        float[] logits = {
                toLogit(0.80f),
                toLogit(0.99f),
                toLogit(0.99f)
        };

        Prediction prediction =
                interpreter.interpret(logits);

        /*
         * Only Dog exists:
         *
         * 0.5 × 0.80 = 0.40
         */
        assertEquals(
                0.40f,
                prediction.confidence(),
                TOLERANCE
        );
    }

    @Test
    void ignoresLogitsBeyondClassMapSize() {
        configureLabels(
                "Dog",
                "Bark",
                "Bow-wow",
                "Yip"
        );

        float[] logits = {
                toLogit(0.40f),
                toLogit(0.40f),
                toLogit(0.40f),
                toLogit(0.40f),

                /*
                 * No matching ClassMap entry exists for this
                 * extremely high extra score.
                 */
                toLogit(0.99f)
        };

        Prediction prediction =
                interpreter.interpret(logits);

        assertEquals(
                0.40f,
                prediction.confidence(),
                TOLERANCE
        );
    }

    @Test
    void rejectsEmptyScoreArray() {


        assertThrows(
                IllegalArgumentException.class,
                () -> interpreter.interpret(
                        new float[0]
                )
        );
    }

    private void configureLabels(
            String... labels
    ) {
        when(classMap.size())
                .thenReturn(labels.length);

        when(classMap.label(anyInt()))
                .thenAnswer(invocation -> {
                    int index =
                            invocation.getArgument(0);

                    if (index < 0
                            || index >= labels.length) {

                        return "class-" + index;
                    }

                    return labels[index];
                });
    }

    private static float toLogit(
            float probability
    ) {
        if (probability <= 0.0f
                || probability >= 1.0f) {

            throw new IllegalArgumentException(
                    "Probability must be between 0 and 1."
            );
        }

        return (float) Math.log(
                probability
                        / (1.0f - probability)
        );
    }
}