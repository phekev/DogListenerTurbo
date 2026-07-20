package com.example.doglistener.ml;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class ClassMap {

    private static final String CLASS_MAP_RESOURCE =
            "model/yamnet_class_map.csv";

    private final List<String> labels;

    public ClassMap() {
        labels = loadLabels();
    }

    public String label(int classIndex) {
        if (classIndex < 0 || classIndex >= labels.size()) {
            return "class-" + classIndex;
        }

        return labels.get(classIndex);
    }

    public int size() {
        return labels.size();
    }

    private List<String> loadLabels() {
        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(CLASS_MAP_RESOURCE);

        if (inputStream == null) {
            throw new IllegalStateException(
                    "Could not find "
                            + CLASS_MAP_RESOURCE
            );
        }

        List<String> loadedLabels =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream
                                )
                        )
        ) {
            // Skip the CSV header.
            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {
                List<String> fields =
                        parseCsvLine(line);

                if (fields.size() < 3) {
                    throw new IllegalStateException(
                            "Malformed class-map row: "
                                    + line
                    );
                }

                loadedLabels.add(fields.get(2));
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to load "
                            + CLASS_MAP_RESOURCE,
                    exception
            );
        }

        if (loadedLabels.isEmpty()) {
            throw new IllegalStateException(
                    "Class map contains no labels."
            );
        }

        return List.copyOf(loadedLabels);
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean insideQuotes = false;

        for (int index = 0;
             index < line.length();
             index++) {

            char character = line.charAt(index);

            if (character == '"') {
                boolean escapedQuote =
                        insideQuotes
                                && index + 1 < line.length()
                                && line.charAt(index + 1)
                                == '"';

                if (escapedQuote) {
                    current.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
            } else if (character == ','
                    && !insideQuotes) {

                fields.add(
                        current.toString().trim()
                );

                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        fields.add(current.toString().trim());

        return fields;
    }
}
