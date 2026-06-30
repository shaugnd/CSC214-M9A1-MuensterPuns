package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Tests command-line input-path resolution for {@link App}.
 */
class AppTest {

    @Test
    void usesDefaultInputPathWhenNoArgumentIsSupplied() {
        Path inputPath = App.resolveInputPath(new String[0]);

        assertEquals(
                Path.of("data", "cheese_data.csv"),
                inputPath);
    }

    @Test
    void usesSuppliedInputPath() {
        Path inputPath = App.resolveInputPath(
                new String[] {"samples/alternate_cheese_data.csv"});

        assertEquals(
                Path.of("samples", "alternate_cheese_data.csv"),
                inputPath);
    }

    @Test
    void rejectsMoreThanOneArgument() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> App.resolveInputPath(
                        new String[] {"first.csv", "second.csv"}));

        assertEquals(
                "Usage: gradlew run --args=\"[input-file]\"",
                exception.getMessage());
    }

    @Test
    void rejectsBlankInputFilename() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> App.resolveInputPath(
                        new String[] {"   "}));

        assertEquals(
                "Input filename cannot be blank.",
                exception.getMessage());
    }

    @Test
    void rejectsNullInputFilename() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> App.resolveInputPath(
                        new String[] {null}));

        assertEquals(
                "Input filename cannot be blank.",
                exception.getMessage());
    }

    @Test
    void rejectsNullArgumentArray() {
        assertThrows(
                NullPointerException.class,
                () -> App.resolveInputPath(null));
    }
}
