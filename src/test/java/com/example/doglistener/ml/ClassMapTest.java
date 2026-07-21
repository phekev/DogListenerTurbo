package com.example.doglistener.ml;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassMapTest {

    @Test
    void loadsBundledYamnetClassMap() {
        ClassMap classMap =
                new ClassMap();

        assertEquals(
                521,
                classMap.size()
        );

        assertEquals(
                "Speech",
                classMap.label(0)
        );

        assertEquals(
                "Dog",
                classMap.label(69)
        );

        assertEquals(
                "Bark",
                classMap.label(70)
        );

        assertEquals(
                "Yip",
                classMap.label(71)
        );

        assertEquals(
                "Howl",
                classMap.label(72)
        );

        assertEquals(
                "Bow-wow",
                classMap.label(73)
        );

        assertEquals(
                "Growling",
                classMap.label(74)
        );

        assertEquals(
                "Whimper (dog)",
                classMap.label(75)
        );
    }

    @Test
    void returnsFallbackForNegativeIndex() {
        ClassMap classMap =
                classMapFromCsv(
                        """
                        index,mid,display_name
                        0,/m/test,Test label
                        """
                );

        assertEquals(
                "class--1",
                classMap.label(-1)
        );
    }

    @Test
    void returnsFallbackForIndexBeyondMap() {
        ClassMap classMap =
                classMapFromCsv(
                        """
                        index,mid,display_name
                        0,/m/test,Test label
                        """
                );

        assertEquals(
                "class-1",
                classMap.label(1)
        );

        assertEquals(
                "class-500",
                classMap.label(500)
        );
    }

    @Test
    void parsesQuotedLabelContainingComma() {
        ClassMap classMap =
                classMapFromCsv(
                        """
                        index,mid,display_name
                        0,/m/test,"Child speech, kid speaking"
                        """
                );

        assertEquals(
                1,
                classMap.size()
        );

        assertEquals(
                "Child speech, kid speaking",
                classMap.label(0)
        );
    }

    @Test
    void parsesEscapedQuoteInsideLabel() {
        ClassMap classMap =
                classMapFromCsv(
                        """
                        index,mid,display_name
                        0,/m/test,"Dog ""warning"" bark"
                        """
                );

        assertEquals(
                "Dog \"warning\" bark",
                classMap.label(0)
        );
    }

    @Test
    void trimsWhitespaceAroundFields() {
        ClassMap classMap =
                classMapFromCsv(
                        """
                        index,mid,display_name
                        0 , /m/test , Dog
                        """
                );

        assertEquals(
                "Dog",
                classMap.label(0)
        );
    }

    @Test
    void rejectsMalformedCsvRow() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> classMapFromCsv(
                                """
                                index,mid,display_name
                                0,/m/test
                                """
                        )
                );

        assertEquals(
                "Malformed class-map row: 0,/m/test",
                exception.getMessage()
        );
    }

    @Test
    void rejectsHeaderOnlyCsv() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> classMapFromCsv(
                                """
                                index,mid,display_name
                                """
                        )
                );

        assertEquals(
                "Class map contains no labels.",
                exception.getMessage()
        );
    }

    @Test
    void rejectsMissingInputStream() {
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> new ClassMap(null)
                );

        assertEquals(
                "Could not find "
                        + "model/yamnet_class_map.csv",
                exception.getMessage()
        );
    }

    @Test
    void wrapsIOExceptionWhileReadingCsv() {
        InputStream brokenStream =
                new InputStream() {

                    @Override
                    public int read()
                            throws IOException {

                        throw new IOException(
                                "Simulated read failure"
                        );
                    }
                };

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> new ClassMap(
                                brokenStream
                        )
                );

        assertEquals(
                "Failed to load "
                        + "model/yamnet_class_map.csv",
                exception.getMessage()
        );

        assertInstanceOf(
                IOException.class,
                exception.getCause()
        );
    }

    private static ClassMap classMapFromCsv(
            String csv
    ) {
        InputStream inputStream =
                new ByteArrayInputStream(
                        csv.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        return new ClassMap(inputStream);
    }
}