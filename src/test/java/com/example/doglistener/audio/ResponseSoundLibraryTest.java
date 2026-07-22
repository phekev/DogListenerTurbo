package com.example.doglistener.audio;

import com.example.doglistener.config.SoundLibraryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseSoundLibraryTest {

    @TempDir
    Path temporaryDirectory;

    private ResponseSoundLibrary library;

    @BeforeEach
    void setUp() {
        SoundLibraryProperties properties =
                new SoundLibraryProperties();

        properties.setSoundDirectory(
                temporaryDirectory
                        .resolve("sounds")
                        .toString()
        );

        library =
                new ResponseSoundLibrary(
                        properties
                );
    }

    @Test
    void initializeCreatesAllResponseDirectories()
            throws IOException {

        library.initialize();

        assertTrue(
                Files.isDirectory(
                        library.getDirectory(
                                ResponseLevel.FIRST
                        )
                )
        );

        assertTrue(
                Files.isDirectory(
                        library.getDirectory(
                                ResponseLevel.SECOND
                        )
                )
        );

        assertTrue(
                Files.isDirectory(
                        library.getDirectory(
                                ResponseLevel.PROLONGED
                        )
                )
        );
    }

    @Test
    void findsArbitrarilyNamedWaveFiles()
            throws IOException {

        library.initialize();

        Path directory =
                library.getDirectory(
                        ResponseLevel.FIRST
                );

        Files.createFile(
                directory.resolve("stop.wav")
        );

        Files.createFile(
                directory.resolve("hey.wav")
        );

        Files.createFile(
                directory.resolve("go away.wav")
        );

        List<String> filenames =
                library.findSounds(
                                ResponseLevel.FIRST
                        )
                        .stream()
                        .map(
                                path -> path
                                        .getFileName()
                                        .toString()
                        )
                        .toList();

        assertEquals(
                List.of(
                        "go away.wav",
                        "hey.wav",
                        "stop.wav"
                ),
                filenames
        );
    }

    @Test
    void acceptsUppercaseWaveExtension()
            throws IOException {

        library.initialize();

        Path sound =
                library.getDirectory(
                                ResponseLevel.SECOND
                        )
                        .resolve("WARNING.WAV");

        Files.createFile(sound);

        assertEquals(
                List.of(sound),
                library.findSounds(
                        ResponseLevel.SECOND
                )
        );
    }

    @Test
    void ignoresNonWaveFilesAndDirectories()
            throws IOException {

        library.initialize();

        Path directory =
                library.getDirectory(
                        ResponseLevel.FIRST
                );

        Path validSound =
                directory.resolve("stop.wav");

        Files.createFile(validSound);
        Files.createFile(
                directory.resolve("notes.txt")
        );
        Files.createFile(
                directory.resolve("recording.mp3")
        );
        Files.createDirectory(
                directory.resolve("nested.wav")
        );

        assertEquals(
                List.of(validSound),
                library.findSounds(
                        ResponseLevel.FIRST
                )
        );
    }

    @Test
    void rescansDirectoryForNewFiles()
            throws IOException {

        library.initialize();

        Path directory =
                library.getDirectory(
                        ResponseLevel.PROLONGED
                );

        Path firstSound =
                directory.resolve("now.wav");

        Files.createFile(firstSound);

        assertEquals(
                List.of(firstSound),
                library.findSounds(
                        ResponseLevel.PROLONGED
                )
        );

        Path secondSound =
                directory.resolve("enough.wav");

        Files.createFile(secondSound);

        assertEquals(
                List.of(
                        secondSound,
                        firstSound
                ),
                library.findSounds(
                        ResponseLevel.PROLONGED
                )
        );
    }

    @Test
    void reflectsRemovedFilesImmediately()
            throws IOException {

        library.initialize();

        Path directory =
                library.getDirectory(
                        ResponseLevel.FIRST
                );

        Path firstSound =
                directory.resolve("hey.wav");

        Path secondSound =
                directory.resolve("stop.wav");

        Files.createFile(firstSound);
        Files.createFile(secondSound);

        Files.delete(firstSound);

        assertEquals(
                List.of(secondSound),
                library.findSounds(
                        ResponseLevel.FIRST
                )
        );
    }

    @Test
    void recreatesDeletedResponseDirectory()
            throws IOException {

        library.initialize();

        Path directory =
                library.getDirectory(
                        ResponseLevel.SECOND
                );

        Files.delete(directory);

        assertTrue(
                library.findSounds(
                        ResponseLevel.SECOND
                ).isEmpty()
        );

        assertTrue(
                Files.isDirectory(directory)
        );
    }

    @Test
    void rejectsNullResponseLevel() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> library.findSounds(null)
                );

        assertEquals(
                "Response level must not be null.",
                exception.getMessage()
        );
    }
}