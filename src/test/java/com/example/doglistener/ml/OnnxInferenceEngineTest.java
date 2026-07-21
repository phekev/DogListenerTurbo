package com.example.doglistener.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnnxInferenceEngineTest {

    @Mock
    private YamnetInputPreprocessor inputPreprocessor;

    @Mock
    private OnnxModelRunner modelRunner;

    @Mock
    private YamnetPredictionInterpreter
            predictionInterpreter;

    private OnnxInferenceEngine inferenceEngine;

    @BeforeEach
    void setUp() {
        inferenceEngine =
                new OnnxInferenceEngine(
                        inputPreprocessor,
                        modelRunner,
                        predictionInterpreter
                );
    }

    @Test
    void runsCompleteInferencePipelineInOrder()
            throws Exception {

        float[] audio =
                new float[16_000];

        float[][][][] modelInput =
                new float[1][1][96][64];

        float[] scores =
                new float[521];

        Prediction expectedPrediction =
                new Prediction(
                        -1,
                        "Dog Bark",
                        0.42f
                );

        when(
                inputPreprocessor.prepare(audio)
        ).thenReturn(modelInput);

        when(
                modelRunner.run(modelInput)
        ).thenReturn(scores);

        when(
                predictionInterpreter.interpret(scores)
        ).thenReturn(expectedPrediction);

        Prediction actualPrediction =
                inferenceEngine.predict(audio);

        assertSame(
                expectedPrediction,
                actualPrediction
        );

        InOrder pipelineOrder =
                inOrder(
                        inputPreprocessor,
                        modelRunner,
                        predictionInterpreter
                );

        pipelineOrder.verify(inputPreprocessor)
                .prepare(audio);

        pipelineOrder.verify(modelRunner)
                .run(modelInput);

        pipelineOrder.verify(predictionInterpreter)
                .interpret(scores);

        verifyNoMoreInteractions(
                inputPreprocessor,
                modelRunner,
                predictionInterpreter
        );
    }

    @Test
    void stopsPipelineWhenPreprocessingFails() {
        float[] audio =
                new float[16_000];

        IllegalArgumentException failure =
                new IllegalArgumentException(
                        "Invalid audio"
                );

        when(
                inputPreprocessor.prepare(audio)
        ).thenThrow(failure);

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> inferenceEngine.predict(audio)
                );

        assertSame(failure, thrown);

        verifyNoMoreInteractions(
                modelRunner,
                predictionInterpreter
        );
    }

    @Test
    void stopsPipelineWhenModelRunnerFails()
            throws Exception {

        float[] audio =
                new float[16_000];

        float[][][][] modelInput =
                new float[1][1][96][64];

        IllegalStateException failure =
                new IllegalStateException(
                        "ONNX inference failed"
                );

        when(
                inputPreprocessor.prepare(audio)
        ).thenReturn(modelInput);

        when(
                modelRunner.run(modelInput)
        ).thenThrow(failure);

        IllegalStateException thrown =
                assertThrows(
                        IllegalStateException.class,
                        () -> inferenceEngine.predict(audio)
                );

        assertSame(failure, thrown);

        verifyNoMoreInteractions(
                predictionInterpreter
        );
    }

    @Test
    void propagatesInterpreterFailure()
            throws Exception {

        float[] audio =
                new float[16_000];

        float[][][][] modelInput =
                new float[1][1][96][64];

        float[] scores =
                new float[521];

        IllegalArgumentException failure =
                new IllegalArgumentException(
                        "Invalid model scores"
                );

        when(
                inputPreprocessor.prepare(audio)
        ).thenReturn(modelInput);

        when(
                modelRunner.run(modelInput)
        ).thenReturn(scores);

        when(
                predictionInterpreter.interpret(scores)
        ).thenThrow(failure);

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> inferenceEngine.predict(audio)
                );

        assertSame(failure, thrown);
    }
}