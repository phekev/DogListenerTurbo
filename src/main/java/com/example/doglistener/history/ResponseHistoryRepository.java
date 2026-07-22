package com.example.doglistener.history;

import com.example.doglistener.audio.ResponseLevel;
import com.example.doglistener.config.ResponseHistoryProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ResponseHistoryRepository {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ResponseHistoryRepository.class
            );

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS response_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                played_at_epoch_ms INTEGER NOT NULL,
                response_level TEXT NOT NULL,
                sound_file TEXT NOT NULL
            )
            """;

    private static final String CREATE_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS
                idx_response_history_played_at
            ON response_history (
                played_at_epoch_ms DESC
            )
            """;

    private final ResponseHistoryProperties properties;

    public ResponseHistoryRepository(
            ResponseHistoryProperties properties
    ) {
        if (properties == null) {
            throw new IllegalArgumentException(
                    "Response history properties "
                            + "must not be null."
            );
        }

        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        Path databasePath =
                getDatabasePath();

        createParentDirectory(databasePath);

        try (
                Connection connection =
                        openConnection();

                Statement statement =
                        connection.createStatement()
        ) {
            statement.execute(CREATE_TABLE_SQL);
            statement.execute(CREATE_INDEX_SQL);

            LOGGER.info(
                    "Response history database ready: {}",
                    databasePath
            );

        } catch (SQLException exception) {
            throw new ResponseHistoryException(
                    "Unable to initialise response "
                            + "history database: "
                            + databasePath,
                    exception
            );
        }
    }

    public void record(
            ResponseLevel responseLevel,
            Path soundFile,
            Instant playedAt
    ) {
        validateRecord(
                responseLevel,
                soundFile,
                playedAt
        );

        Path normalizedSoundFile =
                soundFile
                        .toAbsolutePath()
                        .normalize();

        String sql = """
                INSERT INTO response_history (
                    played_at_epoch_ms,
                    response_level,
                    sound_file
                )
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection =
                        openConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(
                    1,
                    playedAt.toEpochMilli()
            );

            statement.setString(
                    2,
                    responseLevel.name()
            );

            statement.setString(
                    3,
                    normalizedSoundFile.toString()
            );

            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new ResponseHistoryException(
                    "Unable to record response history.",
                    exception
            );
        }
    }

    public ResponseStatistics getStatistics() {
        String sql = """
                SELECT
                    response_level,
                    COUNT(*) AS response_count
                FROM response_history
                GROUP BY response_level
                """;

        long firstResponses = 0L;
        long secondResponses = 0L;
        long prolongedResponses = 0L;

        try (
                Connection connection =
                        openConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {
            while (resultSet.next()) {
                ResponseLevel responseLevel =
                        parseResponseLevel(
                                resultSet.getString(
                                        "response_level"
                                )
                        );

                long count =
                        resultSet.getLong(
                                "response_count"
                        );

                switch (responseLevel) {
                    case FIRST ->
                            firstResponses = count;

                    case SECOND ->
                            secondResponses = count;

                    case PROLONGED ->
                            prolongedResponses = count;
                }
            }

        } catch (SQLException exception) {
            throw new ResponseHistoryException(
                    "Unable to read response statistics.",
                    exception
            );
        }

        long overallResponses =
                firstResponses
                        + secondResponses
                        + prolongedResponses;

        return new ResponseStatistics(
                firstResponses,
                secondResponses,
                prolongedResponses,
                overallResponses
        );
    }

    public List<ResponseHistoryEntry> findRecent(
            int maximumResults
    ) {
        if (maximumResults <= 0) {
            throw new IllegalArgumentException(
                    "Maximum results must be greater "
                            + "than zero."
            );
        }

        String sql = """
                SELECT
                    id,
                    played_at_epoch_ms,
                    response_level,
                    sound_file
                FROM response_history
                ORDER BY
                    played_at_epoch_ms DESC,
                    id DESC
                LIMIT ?
                """;

        List<ResponseHistoryEntry> entries =
                new ArrayList<>();

        try (
                Connection connection =
                        openConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(
                    1,
                    maximumResults
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                while (resultSet.next()) {
                    entries.add(
                            mapEntry(resultSet)
                    );
                }
            }

        } catch (SQLException exception) {
            throw new ResponseHistoryException(
                    "Unable to read response history.",
                    exception
            );
        }

        return List.copyOf(entries);
    }

    public Path getDatabasePath() {
        String configuredFile =
                properties.getDatabaseFile();

        if (configuredFile == null
                || configuredFile.isBlank()) {

            throw new IllegalStateException(
                    "Response history database file "
                            + "must not be blank."
            );
        }

        Path databasePath =
                Path.of(configuredFile.trim());

        if (!databasePath.isAbsolute()) {
            throw new IllegalStateException(
                    "Response history database file "
                            + "must be an absolute path: "
                            + configuredFile
            );
        }

        return databasePath.normalize();
    }

    private Connection openConnection()
            throws SQLException {

        String databaseUrl =
                "jdbc:sqlite:"
                        + getDatabasePath();

        Connection connection =
                DriverManager.getConnection(
                        databaseUrl
                );

        try (
                Statement statement =
                        connection.createStatement()
        ) {
            statement.execute(
                    "PRAGMA busy_timeout = 5000"
            );
        }

        return connection;
    }

    private void createParentDirectory(
            Path databasePath
    ) {
        Path parentDirectory =
                databasePath.getParent();

        if (parentDirectory == null) {
            return;
        }

        try {
            Files.createDirectories(
                    parentDirectory
            );

        } catch (IOException exception) {
            throw new ResponseHistoryException(
                    "Unable to create response history "
                            + "directory: "
                            + parentDirectory,
                    exception
            );
        }
    }

    private void validateRecord(
            ResponseLevel responseLevel,
            Path soundFile,
            Instant playedAt
    ) {
        if (responseLevel == null) {
            throw new IllegalArgumentException(
                    "Response level must not be null."
            );
        }

        if (soundFile == null) {
            throw new IllegalArgumentException(
                    "Sound file must not be null."
            );
        }

        if (playedAt == null) {
            throw new IllegalArgumentException(
                    "Played-at time must not be null."
            );
        }
    }

    private ResponseHistoryEntry mapEntry(
            ResultSet resultSet
    ) throws SQLException {

        return new ResponseHistoryEntry(
                resultSet.getLong("id"),
                Instant.ofEpochMilli(
                        resultSet.getLong(
                                "played_at_epoch_ms"
                        )
                ),
                parseResponseLevel(
                        resultSet.getString(
                                "response_level"
                        )
                ),
                Path.of(
                        resultSet.getString(
                                "sound_file"
                        )
                )
        );
    }

    private ResponseLevel parseResponseLevel(
            String storedValue
    ) {
        try {
            return ResponseLevel.valueOf(
                    storedValue
            );

        } catch (
                NullPointerException
                | IllegalArgumentException exception
        ) {
            throw new IllegalStateException(
                    "Unknown stored response level: "
                            + storedValue,
                    exception
            );
        }
    }
}