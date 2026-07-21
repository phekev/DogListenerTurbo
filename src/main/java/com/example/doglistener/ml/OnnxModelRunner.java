package com.example.doglistener.ml;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

@Component
public class OnnxModelRunner {

    private static final String MODEL_RESOURCE =
            "model/yamnet.onnx";

    private static final String MODEL_DATA_RESOURCE =
            "model/yamnet.data";

    private OrtEnvironment environment;
    private OrtSession session;
    private String inputName;
    private final OnnxScoreExtractor scoreExtractor;

    public OnnxModelRunner(
            OnnxScoreExtractor scoreExtractor
    ) {
        this.scoreExtractor = scoreExtractor;
    }

    @PostConstruct
    public void initialize() throws Exception {
        System.out.println("Initializing ONNX model runner...");

        environment = OrtEnvironment.getEnvironment();

        Path modelPath = extractModel();

        System.out.println(
                "Model extracted to " + modelPath
        );

        session = environment.createSession(
                modelPath.toString(),
                new OrtSession.SessionOptions()
        );

        inputName = session
                .getInputNames()
                .iterator()
                .next();

        System.out.println("ONNX session created.");

        printModelInformation();
    }

    public float[] run(float[][][][] modelInput)
            throws OrtException {

        ensureInitialized();

        try (
                OnnxTensor inputTensor =
                        OnnxTensor.createTensor(
                                environment,
                                modelInput
                        );

                OrtSession.Result result =
                        session.run(
                                Collections.singletonMap(
                                        inputName,
                                        inputTensor
                                )
                        )
        ) {
            return extractScores(result);
        }
    }

    private void ensureInitialized() {
        if (environment == null
                || session == null
                || inputName == null) {

            throw new IllegalStateException(
                    "ONNX model runner is not initialized."
            );
        }
    }

    private Path extractModel() throws IOException {
        Path temporaryDirectory =
                Files.createTempDirectory(
                        "doglistener-"
                );

        copyResource(
                MODEL_RESOURCE,
                temporaryDirectory.resolve(
                        "yamnet.onnx"
                )
        );

        copyResource(
                MODEL_DATA_RESOURCE,
                temporaryDirectory.resolve(
                        "yamnet.data"
                )
        );

        temporaryDirectory
                .toFile()
                .deleteOnExit();

        return temporaryDirectory.resolve(
                "yamnet.onnx"
        );
    }

    private void copyResource(
            String resource,
            Path destination
    ) throws IOException {

        try (
                InputStream inputStream =
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(
                                        resource
                                )
        ) {
            if (inputStream == null) {
                throw new FileNotFoundException(
                        resource
                );
            }

            Files.copy(
                    inputStream,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        destination
                .toFile()
                .deleteOnExit();
    }

    private float[] extractScores(
            OrtSession.Result result
    ) throws OrtException {

        OnnxValue output =
                findScoresOutput(result);

        return scoreExtractor.extract(
                output.getValue()
        );
    }

    private OnnxValue findScoresOutput(
            OrtSession.Result result
    ) {
        return result.get("class_scores")
                .orElseGet(
                        () -> result.get("scores")
                                .orElseGet(
                                        () -> result.get(0)
                                )
                );
    }

    private void printModelInformation()
            throws OrtException {

        System.out.println();
        System.out.println(
                "========== YAMNet Model =========="
        );

        System.out.println("Inputs");

        for (
                var entry
                : session.getInputInfo().entrySet()
        ) {
            String name = entry.getKey();
            NodeInfo information =
                    entry.getValue();

            System.out.println("  " + name);

            if (
                    information.getInfo()
                            instanceof TensorInfo tensorInfo
            ) {
                System.out.println(
                        "    Type  : "
                                + tensorInfo.type
                );

                System.out.print(
                        "    Shape : "
                );

                for (
                        long dimension
                        : tensorInfo.getShape()
                ) {
                    System.out.print(
                            dimension + " "
                    );
                }

                System.out.println();
            }
        }

        System.out.println();
        System.out.println("Outputs");

        for (
                var entry
                : session.getOutputInfo().entrySet()
        ) {
            String name = entry.getKey();
            NodeInfo information =
                    entry.getValue();

            System.out.println("  " + name);

            if (
                    information.getInfo()
                            instanceof TensorInfo tensorInfo
            ) {
                System.out.println(
                        "    Type  : "
                                + tensorInfo.type
                );

                System.out.print(
                        "    Shape : "
                );

                for (
                        long dimension
                        : tensorInfo.getShape()
                ) {
                    System.out.print(
                            dimension + " "
                    );
                }

                System.out.println();
            }
        }

        System.out.println(
                "=================================="
        );
    }

    @PreDestroy
    public void shutdown() throws Exception {
        if (session != null) {
            session.close();
            session = null;
        }

        if (environment != null) {
            environment.close();
            environment = null;
        }

        inputName = null;
    }
}
