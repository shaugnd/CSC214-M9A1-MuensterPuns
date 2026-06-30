package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests human-readable formatting and UTF-8 file output in
 * {@link CheeseReportWriter}.
 */
class CheeseReportWriterTest {
    private final CheeseReportWriter writer = new CheeseReportWriter();

    @TempDir
    Path temporaryDirectory;

    @Test
    void formatsCompletedReport() {
        CheeseStatisticsReport report = new CheeseStatisticsReport(
                1042,
                800,
                115,
                53,
                "Cow",
                748,
                1028,
                47.069747,
                38);

        String expected = """
                Canadian Cheese Analysis
                Cheeses analyzed: 1042
                Pasteurized cheeses: 800
                Raw-milk cheeses: 115
                Organic cheeses above 41.0% moisture: 53
                Most common milk source: Cow (748 cheeses)
                Valid moisture values: 1028
                Average moisture: 47.07%
                Flavour descriptions containing "lactic": 38
                """;

        assertEquals(expected, writer.format(report));
    }

    @Test
    void formatsUnavailableMilkSource() {
        CheeseStatisticsReport report = new CheeseStatisticsReport(
                0,
                0,
                0,
                0,
                "",
                0,
                0,
                0.0,
                0);

        assertTrue(writer.format(report).contains(
                "Most common milk source: Not available"));
    }

    @Test
    void writesUtf8ReportToFile() throws IOException {
        CheeseStatisticsReport report = new CheeseStatisticsReport(
                1,
                1,
                0,
                0,
                "Cow",
                1,
                1,
                47.5,
                0);

        Path output = temporaryDirectory.resolve("output.txt");

        writer.write(output, report);

        assertEquals(
                writer.format(report),
                Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void createsMissingParentDirectories() throws IOException {
        CheeseStatisticsReport report = new CheeseStatisticsReport(
                1,
                1,
                0,
                0,
                "Cow",
                1,
                1,
                47.5,
                0);

        Path output = temporaryDirectory
                .resolve("reports")
                .resolve("analysis")
                .resolve("output.txt");

        writer.write(output, report);

        assertTrue(Files.exists(output));
    }

    @Test
    void overwritesExistingOutputFile() throws IOException {
        CheeseStatisticsReport report = new CheeseStatisticsReport(
                1,
                1,
                0,
                0,
                "Cow",
                1,
                1,
                47.5,
                0);

        Path output = temporaryDirectory.resolve("output.txt");
        Files.writeString(
                output,
                "obsolete content that should disappear",
                StandardCharsets.UTF_8);

        writer.write(output, report);

        assertEquals(
                writer.format(report),
                Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void formatRejectsNullReport() {
        assertThrows(
                NullPointerException.class,
                () -> writer.format(null));
    }

    @Test
    void writeRejectsNullPath() {
        CheeseStatisticsReport report = new CheeseStatisticsReport(
                0,
                0,
                0,
                0,
                "",
                0,
                0,
                0.0,
                0);

        assertThrows(
                NullPointerException.class,
                () -> writer.write(null, report));
    }

    @Test
    void writeRejectsNullReport() {
        Path output = temporaryDirectory.resolve("output.txt");

        assertThrows(
                NullPointerException.class,
                () -> writer.write(output, null));
    }
}
