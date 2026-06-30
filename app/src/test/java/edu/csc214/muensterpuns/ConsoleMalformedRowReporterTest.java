package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

/**
 * Tests diagnostic formatting and validation in
 * {@link ConsoleMalformedRowReporter}.
 */
class ConsoleMalformedRowReporterTest {
    private static final String LINE = System.lineSeparator();

    @Test
    void reportsRowNumberIdAndProblem() {
        StringWriter output = new StringWriter();
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(output);

        reporter.report(
                417,
                "1038",
                "Invalid moisture value \"forty-seven\".");

        assertEquals(
                "Row 417, CheeseId 1038: "
                        + "Invalid moisture value \"forty-seven\"."
                        + LINE,
                output.toString());
    }

    @Test
    void nullRecordIdBecomesUnavailable() {
        StringWriter output = new StringWriter();
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(output);

        reporter.report(
                913,
                null,
                "Unclosed quoted field.");

        assertEquals(
                "Row 913, CheeseId unavailable: "
                        + "Unclosed quoted field."
                        + LINE,
                output.toString());
    }

    @Test
    void blankRecordIdBecomesUnavailable() {
        StringWriter output = new StringWriter();
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(output);

        reporter.report(
                22,
                "   ",
                "Cheese ID is blank.");

        assertEquals(
                "Row 22, CheeseId unavailable: "
                        + "Cheese ID is blank."
                        + LINE,
                output.toString());
    }

    @Test
    void stripsSurroundingWhitespace() {
        StringWriter output = new StringWriter();
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(output);

        reporter.report(
                50,
                "  228  ",
                "  Invalid organic value.  ");

        assertEquals(
                "Row 50, CheeseId 228: "
                        + "Invalid organic value."
                        + LINE,
                output.toString());
    }

    @Test
    void reportsSeveralProblemsImmediately() {
        StringWriter output = new StringWriter();
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(output);

        reporter.report(2, "228", "First problem.");
        reporter.report(3, "229", "Second problem.");

        assertEquals(
                "Row 2, CheeseId 228: First problem." + LINE
                        + "Row 3, CheeseId 229: Second problem." + LINE,
                output.toString());
    }

    @Test
    void constructorRejectsNullWriter() {
        assertThrows(
                NullPointerException.class,
                () -> new ConsoleMalformedRowReporter(null));
    }

    @Test
    void rejectsNonPositiveRowNumber() {
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(new StringWriter());

        assertThrows(
                IllegalArgumentException.class,
                () -> reporter.report(
                        0,
                        "228",
                        "Invalid record."));

        assertThrows(
                IllegalArgumentException.class,
                () -> reporter.report(
                        -1,
                        "228",
                        "Invalid record."));
    }

    @Test
    void rejectsNullProblemDescription() {
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(new StringWriter());

        assertThrows(
                NullPointerException.class,
                () -> reporter.report(
                        2,
                        "228",
                        null));
    }

    @Test
    void rejectsBlankProblemDescription() {
        MalformedRowReporter reporter =
                new ConsoleMalformedRowReporter(new StringWriter());

        assertThrows(
                IllegalArgumentException.class,
                () -> reporter.report(
                        2,
                        "228",
                        "   "));
    }
}