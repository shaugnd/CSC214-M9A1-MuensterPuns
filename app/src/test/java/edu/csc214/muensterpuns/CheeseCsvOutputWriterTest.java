package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests streamed creation of the headerless and ID-free CSV add-on files.
 *
 * <p>The tests cover correct headers and records, CSV quoting preservation,
 * reordered schemas, directory creation, overwrite behavior, path reporting,
 * close behavior, and defensive validation.</p>
 */
class CheeseCsvOutputWriterTest {
    // This is probably unnecessary  for the assignment parameters, but I don't
    // want to lose points for failing to do something simple.  Lots of typing,
    // but not particularly complicated.
    
    private final CsvCodec csvCodec = new CsvCodec();
    private final List<String> header = actualHeader();
    private final CheeseSchema schema = CheeseSchema.fromHeader(header);

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsBothExpectedOutputFiles() throws IOException {
        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(temporaryDirectory, csvCodec, schema, header)) {
        }

        assertTrue(Files.exists(
                temporaryDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_HEADERS_FILENAME)));

        assertTrue(Files.exists(
                temporaryDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME)));
    }

    @Test
    void headerlessFileContainsOnlyDataRows() throws IOException {
        List<String> firstRecord = validRecord("228", "Mild");
        List<String> secondRecord = validRecord("229", "Sharp");

        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(temporaryDirectory, csvCodec, schema, header)) {

            writer.writeRecord(firstRecord);
            writer.writeRecord(secondRecord);
        }

        List<String> lines = Files.readAllLines(
                temporaryDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_HEADERS_FILENAME),
                StandardCharsets.UTF_8);

        assertEquals(2, lines.size());
        assertEquals(firstRecord, csvCodec.parse(lines.get(0)));
        assertEquals(secondRecord, csvCodec.parse(lines.get(1)));
    }

    @Test
    void noIdFileContainsReducedHeaderAndDataRows() throws IOException {
        List<String> record = validRecord("228", "Mild and lactic");

        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(temporaryDirectory, csvCodec, schema, header)) {

            writer.writeRecord(record);
        }

        List<String> lines = Files.readAllLines(
                temporaryDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME),
                StandardCharsets.UTF_8);

        assertEquals(2, lines.size());

        List<String> reducedHeader = csvCodec.parse(lines.get(0));
        List<String> reducedRecord = csvCodec.parse(lines.get(1));

        assertEquals(12, reducedHeader.size());
        assertFalse(reducedHeader.contains("CheeseId"));
        assertEquals("ManufacturerProvCode", reducedHeader.get(0));

        assertEquals(12, reducedRecord.size());
        assertFalse(reducedRecord.contains("228"));
        assertEquals("NB", reducedRecord.get(0));
    }

    @Test
    void preservesQuotedCommasQuotationMarksAndUnicode() throws IOException {
        List<String> record = new ArrayList<>(validRecord("230", "Rich, creamy and \"lactic\""));
        record.set(11, "Crème \"Spéciale\"");

        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(temporaryDirectory, csvCodec, schema, header)) {

            writer.writeRecord(record);
        }

        List<String> headerlessLines = Files.readAllLines(
                temporaryDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_HEADERS_FILENAME),
                StandardCharsets.UTF_8);

        List<String> noIdLines = Files.readAllLines(
                temporaryDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME),
                StandardCharsets.UTF_8);

        assertEquals(record, csvCodec.parse(headerlessLines.get(0)));

        List<String> expectedWithoutId = new ArrayList<>(record);
        expectedWithoutId.remove(schema.cheeseIdIndex());

        assertEquals(expectedWithoutId, csvCodec.parse(noIdLines.get(1)));
    }

    @Test
    void removesIdColumnUsingSchemaRatherThanAssumingFirstColumn()
            throws IOException {

        List<String> reorderedHeader = List.of(
                "Organic",
                "FlavourEn",
                "CheeseId",
                "MilkTypeEn",
                "MoisturePercent",
                "MilkTreatmentTypeEn");

        CheeseSchema reorderedSchema =
                CheeseSchema.fromHeader(reorderedHeader);

        List<String> record = List.of(
                "1",
                "Lactic and sharp",
                "900",
                "Goat",
                "52.5",
                "Raw Milk");

        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        reorderedSchema,
                        reorderedHeader)) {

            writer.writeRecord(record);
        }

        List<String> lines = Files.readAllLines(
                temporaryDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME),
                StandardCharsets.UTF_8);

        assertEquals(
                List.of(
                        "Organic",
                        "FlavourEn",
                        "MilkTypeEn",
                        "MoisturePercent",
                        "MilkTreatmentTypeEn"),
                csvCodec.parse(lines.get(0)));

        assertEquals(
                List.of(
                        "1",
                        "Lactic and sharp",
                        "Goat",
                        "52.5",
                        "Raw Milk"),
                csvCodec.parse(lines.get(1)));
    }

    @Test
    void createsMissingOutputDirectory() throws IOException {
        Path nestedOutputDirectory =
                temporaryDirectory.resolve("generated").resolve("csv");

        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        nestedOutputDirectory,
                        csvCodec,
                        schema,
                        header)) {
        }

        assertTrue(Files.isDirectory(nestedOutputDirectory));
        assertTrue(Files.exists(
                nestedOutputDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_HEADERS_FILENAME)));
        assertTrue(Files.exists(
                nestedOutputDirectory.resolve(
                        CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME)));
    }

    @Test
    void overwritesExistingOutputFiles() throws IOException {
        Path headerlessPath = temporaryDirectory.resolve(
                CheeseCsvOutputWriter.WITHOUT_HEADERS_FILENAME);

        Path noIdPath = temporaryDirectory.resolve(
                CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME);

        Files.writeString(
                headerlessPath,
                "obsolete headerless content",
                StandardCharsets.UTF_8);

        Files.writeString(
                noIdPath,
                "obsolete no-id content",
                StandardCharsets.UTF_8);

        List<String> record = validRecord("228", "Mild");

        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        header)) {

            writer.writeRecord(record);
        }

        List<String> headerlessLines =
                Files.readAllLines(headerlessPath, StandardCharsets.UTF_8);

        List<String> noIdLines =
                Files.readAllLines(noIdPath, StandardCharsets.UTF_8);

        assertEquals(1, headerlessLines.size());
        assertEquals(record, csvCodec.parse(headerlessLines.get(0)));

        assertEquals(2, noIdLines.size());
        assertEquals(12, csvCodec.parse(noIdLines.get(0)).size());
        assertEquals(12, csvCodec.parse(noIdLines.get(1)).size());
    }

    @Test
    void reportsResolvedOutputPaths() throws IOException {
        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        header)) {

            assertEquals(
                    temporaryDirectory.resolve(
                            CheeseCsvOutputWriter.WITHOUT_HEADERS_FILENAME),
                    writer.withoutHeadersPath());

            assertEquals(
                    temporaryDirectory.resolve(
                            CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME),
                    writer.withoutIdsPath());
        }
    }

    @Test
    void closeCanBeCalledMoreThanOnce() throws IOException {
        CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        header);

        writer.close();
        writer.close();
    }

    @Test
    void writeAfterCloseIsRejected() throws IOException {
        CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        header);

        writer.close();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> writer.writeRecord(validRecord("228", "Mild")));

        assertEquals(
                "CSV output writer is closed.",
                exception.getMessage());
    }

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(
                NullPointerException.class,
                () -> new CheeseCsvOutputWriter(
                        null,
                        csvCodec,
                        schema,
                        header));

        assertThrows(
                NullPointerException.class,
                () -> new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        null,
                        schema,
                        header));

        assertThrows(
                NullPointerException.class,
                () -> new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        null,
                        header));

        assertThrows(
                NullPointerException.class,
                () -> new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        null));
    }

    @Test
    void constructorRejectsHeaderWithWrongFieldCount() {
        List<String> shortHeader = new ArrayList<>(header);
        shortHeader.removeLast();

        assertThrows(
                IllegalArgumentException.class,
                () -> new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        shortHeader));
    }

    @Test
    void writeRejectsRecordWithWrongFieldCount() throws IOException {
        List<String> shortRecord = validRecord("228", "Mild");
        shortRecord.removeLast();

        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        header)) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> writer.writeRecord(shortRecord));
        }
    }

    @Test
    void writeRejectsNullRecord() throws IOException {
        try (CheeseCsvOutputWriter writer =
                new CheeseCsvOutputWriter(
                        temporaryDirectory,
                        csvCodec,
                        schema,
                        header)) {

            assertThrows(
                    NullPointerException.class,
                    () -> writer.writeRecord(null));
        }
    }

    private static List<String> actualHeader() {
        return List.of(
                "CheeseId",
                "ManufacturerProvCode",
                "ManufacturingTypeEn",
                "MoisturePercent",
                "FlavourEn",
                "CharacteristicsEn",
                "Organic",
                "CategoryTypeEn",
                "MilkTypeEn",
                "MilkTreatmentTypeEn",
                "RindTypeEn",
                "CheeseName",
                "FatLevel");
    }

    private static List<String> validRecord(
            String id,
            String flavour) {

        return new ArrayList<>(List.of(
                id,
                "NB",
                "Farmstead",
                "47.0",
                flavour,
                "Creamy",
                "0",
                "Semi-soft",
                "Cow",
                "Pasteurized",
                "Washed",
                "Sample Cheese",
                "lower fat"));
    }
}
