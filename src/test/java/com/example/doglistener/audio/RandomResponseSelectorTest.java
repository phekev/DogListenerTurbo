package com.example.doglistener.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomResponseSelectorTest {

    @Mock
    private ResponseSoundLibrary soundLibrary;

    @Mock
    private RandomGenerator randomGenerator;

    private RandomResponseSelector selector;

    @BeforeEach
    void setUp() {
        selector =
                new RandomResponseSelector(
                        soundLibrary,
                        randomGenerator
                );
    }

    @Test
    void returnsEmptyWhenNoSoundsExist()
            throws IOException {

        Path responseDirectory =
                Path.of(
                        "sounds",
                        "response_one"
                );

        when(
                soundLibrary.findSounds(
                        ResponseLevel.FIRST
                )
        ).thenReturn(List.of());

        when(
                soundLibrary.getDirectory(
                        ResponseLevel.FIRST
                )
        ).thenReturn(responseDirectory);

        Optional<Path> selected =
                selector.select(
                        ResponseLevel.FIRST
                );

        assertEquals(
                Optional.empty(),
                selected
        );

        verifyNoInteractions(randomGenerator);
    }

    @Test
    void returnsOnlyAvailableSoundWithoutRandomSelection()
            throws IOException {

        Path sound =
                Path.of("stop.wav");

        when(
                soundLibrary.findSounds(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                List.of(sound)
        );

        assertEquals(
                Optional.of(sound),
                selector.select(
                        ResponseLevel.FIRST
                )
        );

        verifyNoInteractions(randomGenerator);
    }

    @Test
    void selectsSoundUsingRandomIndex()
            throws IOException {

        Path first =
                Path.of("hey.wav");

        Path second =
                Path.of("stop.wav");

        Path third =
                Path.of("quiet.wav");

        when(
                soundLibrary.findSounds(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                List.of(
                        first,
                        second,
                        third
                )
        );

        when(
                randomGenerator.nextInt(3)
        ).thenReturn(1);

        assertEquals(
                Optional.of(second),
                selector.select(
                        ResponseLevel.FIRST
                )
        );
    }

    @Test
    void avoidsImmediateRepeatWhenAlternativesExist()
            throws IOException {

        Path first =
                Path.of("hey.wav");

        Path second =
                Path.of("stop.wav");

        Path third =
                Path.of("quiet.wav");

        List<Path> sounds =
                List.of(
                        first,
                        second,
                        third
                );

        when(
                soundLibrary.findSounds(
                        ResponseLevel.FIRST
                )
        ).thenReturn(sounds);

        /*
         * First selection:
         * index 1 from [first, second, third]
         * selects second.
         */
        when(
                randomGenerator.nextInt(3)
        ).thenReturn(1);

        /*
         * Second selection:
         * previous second is removed, leaving
         * [first, third]. Index 1 selects third.
         */
        when(
                randomGenerator.nextInt(2)
        ).thenReturn(1);

        assertEquals(
                Optional.of(second),
                selector.select(
                        ResponseLevel.FIRST
                )
        );

        assertEquals(
                Optional.of(third),
                selector.select(
                        ResponseLevel.FIRST
                )
        );
    }

    @Test
    void maintainsSeparateHistoryForEachResponseLevel()
            throws IOException {

        Path firstLevelOne =
                Path.of("first-hey.wav");

        Path firstLevelTwo =
                Path.of("first-stop.wav");

        Path secondLevelOne =
                Path.of("second-now.wav");

        Path secondLevelTwo =
                Path.of("second-enough.wav");

        when(
                soundLibrary.findSounds(
                        ResponseLevel.FIRST
                )
        ).thenReturn(
                List.of(
                        firstLevelOne,
                        firstLevelTwo
                )
        );

        when(
                soundLibrary.findSounds(
                        ResponseLevel.SECOND
                )
        ).thenReturn(
                List.of(
                        secondLevelOne,
                        secondLevelTwo
                )
        );

        when(
                randomGenerator.nextInt(2)
        ).thenReturn(
                0,
                0
        );

        when(
                randomGenerator.nextInt(1)
        ).thenReturn(0);

        assertEquals(
                Optional.of(firstLevelOne),
                selector.select(
                        ResponseLevel.FIRST
                )
        );

        assertEquals(
                Optional.of(secondLevelOne),
                selector.select(
                        ResponseLevel.SECOND
                )
        );

        /*
         * FIRST remembers firstLevelOne and therefore
         * must choose firstLevelTwo next.
         */
        assertEquals(
                Optional.of(firstLevelTwo),
                selector.select(
                        ResponseLevel.FIRST
                )
        );
    }

    @Test
    void handlesPreviouslySelectedSoundBeingRemoved()
            throws IOException {

        Path first =
                Path.of("hey.wav");

        Path second =
                Path.of("stop.wav");

        Path replacement =
                Path.of("enough.wav");

        when(
                soundLibrary.findSounds(
                        ResponseLevel.PROLONGED
                )
        ).thenReturn(
                List.of(first, second),
                List.of(second, replacement)
        );

        /*
         * First call selects first.
         * Second call sees that first was removed,
         * so both remaining files are candidates.
         */
        when(
                randomGenerator.nextInt(2)
        ).thenReturn(
                0,
                1
        );

        assertEquals(
                Optional.of(first),
                selector.select(
                        ResponseLevel.PROLONGED
                )
        );

        assertEquals(
                Optional.of(replacement),
                selector.select(
                        ResponseLevel.PROLONGED
                )
        );
    }

    @Test
    void rejectsNullResponseLevel()
            throws IOException {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> selector.select(null)
                );

        assertEquals(
                "Response level must not be null.",
                exception.getMessage()
        );

        verify(
                soundLibrary,
                never()
        ).findSounds(
                org.mockito.ArgumentMatchers.any()
        );

        verifyNoInteractions(randomGenerator);
    }
}