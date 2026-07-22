package com.example.doglistener.history;

import com.example.doglistener.audio.ResponseLevel;
import com.example.doglistener.config.ResponseHistoryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseHistoryRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    private Path databaseFile;
    private ResponseHistoryProperties properties;
    private ResponseHistoryRepository repository;

    @BeforeEach
    void setUp() {
        databaseFile =
                temporaryDirectory
                        .resolve("logs")
                        .resolve("response-history.db")
                        .toAbsolutePath()
                        .normalize();

        properties =
                new ResponseHistoryProperties();

        properties.setDatabaseFile(
                databaseFile.toString()
        );

        repository =
                new ResponseHistoryRepository(
                        properties
                );

        repository.initialize();
    }

    @Test
    void initializeCreatesDatabaseAndDirectory() {
        assertTrue(
                Files.isDirectory(
                        databaseFile.getParent()
                )
        );

        assertTrue(
                Files.isRegularFile(
                        databaseFile
                )
        );
    }

    @Test
    void emptyHistoryReturnsZeroStatistics() {
        ResponseStatistics statistics =
                repository.getStatistics();

        assertEquals(
                0L,
                statistics.firstResponses()
        );

        assertEquals(
                0L,
                statistics.secondResponses()
        );

        assertEquals(
                0L,
                statistics.prolongedResponses()
        );

        assertEquals(
                0L,
                statistics.overallResponses()
        );
    }

    @Test
    void recordsCountsForEveryResponseLevel() {
        repository.record(
                ResponseLevel.FIRST,
                sound("first.wav"),
                Instant.parse(
                        "2026-07-22T10:00:00Z"
                )
        );

        repository.record(
                ResponseLevel.FIRST,
                sound("another-first.wav"),
                Instant.parse(
                        "2026-07-22T10:01:00Z"
                )
        );

        repository.record(
                ResponseLevel.SECOND,
                sound("second.wav"),
                Instant.parse(
                        "2026-07-22T10:02:00Z"
                )
        );

        repository.record(
                ResponseLevel.PROLONGED,
                sound("prolonged.wav"),
                Instant.parse(
                        "2026-07-22T10:03:00Z"
                )
        );

        ResponseStatistics statistics =
                repository.getStatistics();

        assertEquals(
                2L,
                statistics.firstResponses()
        );

        assertEquals(
                1L,
                statistics.secondResponses()
        );

        assertEquals(
                1L,
                statistics.prolongedResponses()
        );

        assertEquals(
                4L,
                statistics.overallResponses()
        );
    }

    @Test
    void returnsMostRecentEntriesFirst() {
        repository.record(
                ResponseLevel.FIRST,
                sound("first.wav"),
                Instant.parse(
                        "2026-07-22T10:00:00Z"
                )
        );

        repository.record(
                ResponseLevel.SECOND,
                sound("second.wav"),
                Instant.parse(
                        "2026-07-22T10:05:00Z"
                )
        );

        repository.record(
                ResponseLevel.PROLONGED,
                sound("third.wav"),
                Instant.parse(
                        "2026-07-22T10:10:00Z"
                )
        );

        List<ResponseHistoryEntry> entries =
                repository.findRecent(10);

        assertEquals(
                3,
                entries.size()
        );

        assertEquals(
                ResponseLevel.PROLONGED,
                entries.get(0).responseLevel()
        );

        assertEquals(
                ResponseLevel.SECOND,
                entries.get(1).responseLevel()
        );

        assertEquals(
                ResponseLevel.FIRST,
                entries.get(2).responseLevel()
        );
    }

    @Test
    void limitsNumberOfReturnedEntries() {
        repository.record(
                ResponseLevel.FIRST,
                sound("first.wav"),
                Instant.parse(
                        "2026-07-22T10:00:00Z"
                )
        );

        repository.record(
                ResponseLevel.SECOND,
                sound("second.wav"),
                Instant.parse(
                        "2026-07-22T10:01:00Z"
                )
        );

        repository.record(
                ResponseLevel.PROLONGED,
                sound("third.wav"),
                Instant.parse(
                        "2026-07-22T10:02:00Z"
                )
        );

        List<ResponseHistoryEntry> entries =
                repository.findRecent(2);

        assertEquals(
                2,
                entries.size()
        );

        assertEquals(
                ResponseLevel.PROLONGED,
                entries.get(0).responseLevel()
        );

        assertEquals(
                ResponseLevel.SECOND,
                entries.get(1).responseLevel()
        );
    }

    @Test
    void historyPersistsAcrossRepositoryInstances() {
        repository.record(
                ResponseLevel.SECOND,
                sound("second.wav"),
                Instant.parse(
                        "2026-07-22T10:00:00Z"
                )
        );

        ResponseHistoryRepository newRepository =
                new ResponseHistoryRepository(
                        properties
                );

        newRepository.initialize();

        ResponseStatistics statistics =
                newRepository.getStatistics();

        assertEquals(
                1L,
                statistics.secondResponses()
        );

        assertEquals(
                1L,
                statistics.overallResponses()
        );
    }

    @Test
    void rejectsInvalidRecordValues() {
        Path soundFile =
                sound("sound.wav");

        Instant playedAt =
                Instant.parse(
                        "2026-07-22T10:00:00Z"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.record(
                        null,
                        soundFile,
                        playedAt
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.record(
                        ResponseLevel.FIRST,
                        null,
                        playedAt
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.record(
                        ResponseLevel.FIRST,
                        soundFile,
                        null
                )
        );
    }

    @Test
    void rejectsInvalidHistoryLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.findRecent(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.findRecent(-1)
        );
    }

    @Test
    void returnsCountForRequestedLevel() {
        repository.record(
                ResponseLevel.FIRST,
                sound("first.wav"),
                Instant.parse(
                        "2026-07-22T10:00:00Z"
                )
        );

        ResponseStatistics statistics =
                repository.getStatistics();

        assertEquals(
                1L,
                statistics.countFor(
                        ResponseLevel.FIRST
                )
        );

        assertEquals(
                0L,
                statistics.countFor(
                        ResponseLevel.SECOND
                )
        );
    }

    private Path sound(String filename) {
        return temporaryDirectory
                .resolve("sounds")
                .resolve(filename)
                .toAbsolutePath()
                .normalize();
    }
}