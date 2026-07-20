package com.example.doglistener.ml;

import org.springframework.stereotype.Component;

@Component
public class OnnxInferenceEngine
        implements InferenceEngine {

    private final YamnetInputPreprocessor
            inputPreprocessor;

    private final OnnxModelRunner modelRunner;

    private final YamnetPredictionInterpreter
            predictionInterpreter;

    public OnnxInferenceEngine(
            YamnetInputPreprocessor inputPreprocessor,
            OnnxModelRunner modelRunner,
            YamnetPredictionInterpreter
                    predictionInterpreter
    ) {
        this.inputPreprocessor =
                inputPreprocessor;

        this.modelRunner = modelRunner;

        this.predictionInterpreter =
                predictionInterpreter;
    }

    @Override
    public Prediction predict(float[] audio)
            throws Exception {

        float[][][][] modelInput =
                inputPreprocessor.prepare(audio);

        float[] logits =
                modelRunner.run(modelInput);

        return predictionInterpreter.interpret(
                logits
        );
    }
}
