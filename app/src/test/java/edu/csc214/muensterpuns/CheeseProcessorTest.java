package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the complete streaming workflow coordinated by {@link CheeseProcessor}.
 */
class CheeseProcessorTest {
    private static final String HEADER =
            "CheeseId,ManufacturerProvCode,ManufacturingTypeEn,"
                    + "MoisturePercent,FlavourEn,CharacteristicsEn,"
                    + "Organic,CategoryTypeEn,MilkTypeEn,"
                    + "MilkTreatmentTypeEn,RindTypeEn,CheeseName,FatLevel";

    @TempDir
    Path temporaryDirectory;

    @Test
    void processesValidAndPartiallyMalformedRowsInOnePass() throws IOException {
        Path inputPath = temporaryDirectory.resolve("cheese_data.csv");
        Path outputDirectory = temporaryDirectory.resolve("generated");
        List<Diagnostic> diagnostics = new ArrayList<>();

        Files.writeString(
                inputPath,
                String.join(
                        System.lineSeparator(),
                        HEADER,
                        "1,ON,Industrial,50.0,\"Fresh, lactic finish\",Soft,1,"
                                + "Firm Cheese,Cow,Pasteurized,Natural,Alpha,Regular",
                        "3,QC,Artisan,bad,Lactically tangy,Firm,0,"
                                + "Soft Cheese,Goat,Raw Milk,Washed,Beta,Regular",
                        "4,BC,Artisan,42.0,Clean,Smooth,yes,"
                                + "Firm Cheese,Cow and Goat,Pasteurized,Natural,Gamma,Regular",
                        "ABC,AB,Industrial,40.0,Mild,Firm,0,"
                                + "Firm Cheese,Cow,Pasteurized,Natural,Delta,Regular",
                        "5,QC")
                        + System.lineSeparator(),
                StandardCharsets.UTF_8);

        CheeseProcessor processor = new CheeseProcessor(
                new CsvCodec(),
                new CheeseParser(),
                new CheeseReportWriter(),
                new MissingIdWriter(),
                (row, id, problem) ->
                        diagnostics.add(new Diagnostic(row, id, problem)));

        CheeseProcessingResult result =
                processor.process(inputPath, outputDirectory);

        CheeseStatisticsReport report = result.statisticsReport();

        assertEquals(3, report.cheesesAnalyzed());
        assertEquals(2, report.pasteurizedCount());
        assertEquals(1, report.rawMilkCount());
        assertEquals(1, report.organicHighMoistureCount());
        assertEquals("Cow", report.mostCommonMilkSource());
        assertEquals(2, report.mostCommonMilkSourceCount());
        assertEquals(2, report.validMoistureCount());
        assertEquals(46.0, report.averageMoisturePercent(), 0.000001);
        assertEquals(1, report.lacticFlavourCount());

        assertEquals(4, result.malformedRowCount());
        assertEquals(1, result.missingIdCount());
        assertEquals(List.of("2"), readLines(result.missingIdsPath()));

        assertEquals(4, diagnostics.size());
        assertEquals(3, diagnostics.get(0).fileRowNumber());
        assertEquals("3", diagnostics.get(0).recordId());
        assertTrue(diagnostics.get(0).problem().contains("Invalid moisture"));

        assertEquals(4, diagnostics.get(1).fileRowNumber());
        assertEquals("4", diagnostics.get(1).recordId());
        assertTrue(diagnostics.get(1).problem().contains("Invalid organic"));

        assertEquals(5, diagnostics.get(2).fileRowNumber());
        assertEquals("ABC", diagnostics.get(2).recordId());
        assertTrue(diagnostics.get(2).problem().contains("Invalid cheese ID"));

        assertEquals(6, diagnostics.get(3).fileRowNumber());
        assertEquals("5", diagnostics.get(3).recordId());
        assertFalse(diagnostics.get(3).problem().isBlank());

        List<String> headerlessRows =
                readLines(result.withoutHeadersPath());

        assertEquals(4, headerlessRows.size());
        assertTrue(headerlessRows.get(0).startsWith("1,"));
        assertTrue(headerlessRows.get(3).startsWith("ABC,"));

        List<String> withoutIdRows =
                readLines(result.withoutIdsPath());

        assertEquals(5, withoutIdRows.size());

        CsvCodec codec = new CsvCodec();
        List<String> noIdHeader = codec.parse(withoutIdRows.get(0));

        assertEquals(12, noIdHeader.size());
        assertEquals("ManufacturerProvCode", noIdHeader.get(0));

        for (String row : withoutIdRows.subList(1, withoutIdRows.size())) {
            assertEquals(12, codec.parse(row).size());
        }

        String reportText = Files.readString(
                result.reportPath(),
                StandardCharsets.UTF_8);

        assertTrue(reportText.contains("Cheeses analyzed: 3"));
        assertTrue(reportText.contains("Pasteurized cheeses: 2"));
        assertTrue(reportText.contains("Raw-milk cheeses: 1"));
        assertTrue(reportText.contains("Average moisture: 46.00%"));
    }

    @Test
    void rejectsEmptyInputFile() throws IOException {
        Path inputPath = temporaryDirectory.resolve("empty.csv");
        Path outputDirectory = temporaryDirectory.resolve("output");
        Files.writeString(inputPath, "", StandardCharsets.UTF_8);

        CheeseProcessor processor = new CheeseProcessor(
                new CsvCodec(),
                new CheeseParser(),
                new CheeseReportWriter(),
                new MissingIdWriter(),
                (row, id, problem) -> { });

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(inputPath, outputDirectory));

        assertEquals("Input CSV file is empty.", exception.getMessage());
    }

    @Test
    void rejectsNullPaths() {
        CheeseProcessor processor = new CheeseProcessor(
                new CsvCodec(),
                new CheeseParser(),
                new CheeseReportWriter(),
                new MissingIdWriter(),
                (row, id, problem) -> { });

        assertThrows(
                NullPointerException.class,
                () -> processor.process(
                        null,
                        temporaryDirectory));

        assertThrows(
                NullPointerException.class,
                () -> processor.process(
                        temporaryDirectory.resolve("input.csv"),
                        null));
    }

    private static List<String> readLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    private record Diagnostic(
            long fileRowNumber,
            String recordId,
            String problem) {
    }
}
