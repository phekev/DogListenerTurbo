package com.example.doglistener.ml;

import org.springframework.stereotype.Component;

@Component
public class OnnxScoreExtractor {

    public float[] extract(Object value) {
        if (value == null) {
            throw new IllegalStateException(
                    "Model returned a null score tensor."
            );
        }

        if (value instanceof float[] scores) {
            return validateOneDimensionalScores(scores);
        }

        if (value instanceof float[][] scores) {
            return averageRows(scores);
        }

        if (value instanceof float[][][] scores) {
            return averageThreeDimensionalScores(scores);
        }

        throw new IllegalStateException(
                "Unsupported model output type: "
                        + value.getClass().getName()
        );
    }

    private float[] validateOneDimensionalScores(
            float[] scores
    ) {
        if (scores.length == 0) {
            throw new IllegalStateException(
                    "Model returned an empty score tensor."
            );
        }

        return scores;
    }

    private float[] averageRows(float[][] rows) {
        if (rows.length == 0) {
            throw new IllegalStateException(
                    "Model returned an empty score tensor."
            );
        }

        validateRow(rows[0], -1);

        if (rows.length == 1) {
            return rows[0];
        }

        int classCount = rows[0].length;

        float[] averages =
                new float[classCount];

        for (int rowIndex = 0;
             rowIndex < rows.length;
             rowIndex++) {

            float[] row = rows[rowIndex];

            validateRow(row, rowIndex);

            if (row.length != classCount) {
                throw new IllegalStateException(
                        "Inconsistent model output dimensions."
                );
            }

            addRow(
                    averages,
                    row
            );
        }

        divideBy(
                averages,
                rows.length
        );

        return averages;
    }

    private float[] averageThreeDimensionalScores(
            float[][][] matrices
    ) {
        if (matrices.length == 0) {
            throw new IllegalStateException(
                    "Model returned an empty score tensor."
            );
        }

        int classCount = -1;
        int rowCount = 0;

        float[] totals = null;

        for (int matrixIndex = 0;
             matrixIndex < matrices.length;
             matrixIndex++) {

            float[][] matrix =
                    matrices[matrixIndex];

            if (matrix == null) {
                throw new IllegalStateException(
                        "Model returned a null score matrix."
                );
            }

            for (int rowIndex = 0;
                 rowIndex < matrix.length;
                 rowIndex++) {

                float[] row =
                        matrix[rowIndex];

                validateRow(row, rowIndex);

                if (classCount < 0) {
                    classCount = row.length;
                    totals = new float[classCount];
                } else if (row.length != classCount) {
                    throw new IllegalStateException(
                            "Inconsistent model output dimensions."
                    );
                }

                addRow(
                        totals,
                        row
                );

                rowCount++;
            }
        }

        if (rowCount == 0) {
            throw new IllegalStateException(
                    "Model returned no score rows."
            );
        }

        divideBy(
                totals,
                rowCount
        );

        return totals;
    }

    private void validateRow(
            float[] row,
            int rowIndex
    ) {
        if (row == null) {
            throw new IllegalStateException(
                    "Model returned a null score row"
                            + formatRowIndex(rowIndex)
                            + "."
            );
        }

        if (row.length == 0) {
            throw new IllegalStateException(
                    "Model returned an empty score row"
                            + formatRowIndex(rowIndex)
                            + "."
            );
        }
    }

    private String formatRowIndex(int rowIndex) {
        if (rowIndex < 0) {
            return "";
        }

        return " at index " + rowIndex;
    }

    private void addRow(
            float[] totals,
            float[] row
    ) {
        for (int classIndex = 0;
             classIndex < totals.length;
             classIndex++) {

            totals[classIndex] +=
                    row[classIndex];
        }
    }

    private void divideBy(
            float[] values,
            int divisor
    ) {
        for (int classIndex = 0;
             classIndex < values.length;
             classIndex++) {

            values[classIndex] /=
                    divisor;
        }
    }
}