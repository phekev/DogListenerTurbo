package com.example.doglistener.audio;

import com.example.doglistener.config.SoundLibraryProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
public class ResponseSoundLibrary {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ResponseSoundLibrary.class
            );

    private final SoundLibraryProperties properties;

    public ResponseSoundLibrary(
            SoundLibraryProperties properties
    ) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() throws IOException {
        Path rootDirectory =
                getRootDirectory();

        Files.createDirectories(rootDirectory);

        for (ResponseLevel level
                : ResponseLevel.values()) {

            Path responseDirectory =
                    getDirectory(level);

            Files.createDirectories(
                    responseDirectory
            );

            LOGGER.info(
                    "Response sound directory ready: {}",
                    responseDirectory.toAbsolutePath()
            );
        }
    }

    public List<Path> findSounds(
            ResponseLevel level
    ) throws IOException {

        if (level == null) {
            throw new IllegalArgumentException(
                    "Response level must not be null."
            );
        }

        Path responseDirectory =
                getDirectory(level);

        /*
         * Recreate the directory if a user has deleted it
         * while the application is running.
         */
        Files.createDirectories(
                responseDirectory
        );

        try (
                Stream<Path> files =
                        Files.list(responseDirectory)
        ) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(
                            ResponseSoundLibrary
                                    ::isWaveFile
                    )
                    .sorted(
                            Comparator.comparing(
                                    path -> path
                                            .getFileName()
                                            .toString()
                                            .toLowerCase(
                                                    Locale.ROOT
                                            )
                            )
                    )
                    .toList();
        }
    }

    public Path getDirectory(
            ResponseLevel level
    ) {
        if (level == null) {
            throw new IllegalArgumentException(
                    "Response level must not be null."
            );
        }

        return getRootDirectory()
                .resolve(
                        level.getDirectoryName()
                );
    }

    private Path getRootDirectory() {
        String configuredDirectory =
                properties.getSoundDirectory();

        if (configuredDirectory == null
                || configuredDirectory.isBlank()) {

            throw new IllegalStateException(
                    "Response sound directory "
                            + "must not be blank."
            );
        }

        Path rootDirectory =
                Path.of(configuredDirectory.trim());

        if (!rootDirectory.isAbsolute()) {
            throw new IllegalStateException(
                    "Response sound directory must be "
                            + "an absolute path: "
                            + configuredDirectory
            );
        }

        return rootDirectory.normalize();
    }

    private static boolean isWaveFile(
            Path file
    ) {
        String filename =
                file.getFileName()
                        .toString()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return filename.endsWith(".wav");
    }
}