package com.example.doglistener.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class RandomResponseSelector {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    RandomResponseSelector.class
            );

    private final ResponseSoundLibrary soundLibrary;
    private final RandomGenerator randomGenerator;

    private final Map<ResponseLevel, Path>
            previousSelections =
            new EnumMap<>(ResponseLevel.class);

    @Autowired
    public RandomResponseSelector(
            ResponseSoundLibrary soundLibrary
    ) {
        this(
                soundLibrary,
                ThreadLocalRandom.current()
        );
    }

    /*
     * Package-private constructor for deterministic
     * unit testing.
     */
    RandomResponseSelector(
            ResponseSoundLibrary soundLibrary,
            RandomGenerator randomGenerator
    ) {
        if (soundLibrary == null) {
            throw new IllegalArgumentException(
                    "Response sound library must not be null."
            );
        }

        if (randomGenerator == null) {
            throw new IllegalArgumentException(
                    "Random generator must not be null."
            );
        }

        this.soundLibrary = soundLibrary;
        this.randomGenerator = randomGenerator;
    }

    public synchronized Optional<Path> select(
            ResponseLevel level
    ) throws IOException {

        if (level == null) {
            throw new IllegalArgumentException(
                    "Response level must not be null."
            );
        }

        List<Path> availableSounds =
                soundLibrary.findSounds(level);

        if (availableSounds.isEmpty()) {
            LOGGER.warn(
                    "No WAV response sounds found "
                            + "for response level {} in {}",
                    level,
                    soundLibrary
                            .getDirectory(level)
                            .toAbsolutePath()
            );

            return Optional.empty();
        }

        if (availableSounds.size() == 1) {
            Path selectedSound =
                    availableSounds.get(0);

            previousSelections.put(
                    level,
                    selectedSound
            );

            return Optional.of(selectedSound);
        }

        Path previousSelection =
                previousSelections.get(level);

        List<Path> candidates =
                removePreviousSelection(
                        availableSounds,
                        previousSelection
                );

        int selectedIndex =
                randomGenerator.nextInt(
                        candidates.size()
                );

        Path selectedSound =
                candidates.get(selectedIndex);

        previousSelections.put(
                level,
                selectedSound
        );

        return Optional.of(selectedSound);
    }

    private List<Path> removePreviousSelection(
            List<Path> availableSounds,
            Path previousSelection
    ) {
        if (previousSelection == null
                || !availableSounds.contains(
                previousSelection
        )) {

            return availableSounds;
        }

        return availableSounds
                .stream()
                .filter(
                        sound -> !sound.equals(
                                previousSelection
                        )
                )
                .toList();
    }
}