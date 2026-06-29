package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests header mapping and record-width validation in {@link CheeseSchema}.
 *
 * <p>The tests cover the real dataset layout, reordered columns, UTF-8 byte
 * order marks, whitespace, missing and duplicate headers, and invalid field
 * counts.</p>
 */
class CheeseSchemaTest {
    // Might as well test everything.  This is what I would do with 
    // production code.
    @Test
    void mapsActualDatasetHeader() {
        CheeseSchema schema = CheeseSchema.fromHeader(actualHeader());

        assertEquals(13, schema.columnCount());
        assertEquals(0, schema.cheeseIdIndex());
        assertEquals(3, schema.moisturePercentIndex());
        assertEquals(4, schema.flavourIndex());
        assertEquals(6, schema.organicIndex());
        assertEquals(8, schema.milkTypeIndex());
        assertEquals(9, schema.milkTreatmentTypeIndex());
    }

    @Test
    void mapsRequiredHeadersWhenColumnsAreReordered() {
        List<String> header = List.of(
                "Organic",
                "FlavourEn",
                "MilkTreatmentTypeEn",
                "CheeseId",
                "MilkTypeEn",
                "MoisturePercent");

        CheeseSchema schema = CheeseSchema.fromHeader(header);

        assertEquals(6, schema.columnCount());
        assertEquals(3, schema.cheeseIdIndex());
        assertEquals(5, schema.moisturePercentIndex());
        assertEquals(1, schema.flavourIndex());
        assertEquals(0, schema.organicIndex());
        assertEquals(4, schema.milkTypeIndex());
        assertEquals(2, schema.milkTreatmentTypeIndex());
    }

    @Test
    void acceptsUtf8BomBeforeFirstHeader() {
        List<String> header = new ArrayList<>(actualHeader());
        header.set(0, "\uFEFFCheeseId");

        CheeseSchema schema = CheeseSchema.fromHeader(header);

        assertEquals(0, schema.cheeseIdIndex());
    }

    @Test
    void ignoresSurroundingWhitespaceInHeaderNames() {
        List<String> header = List.of(
                " CheeseId ",
                " MoisturePercent ",
                " FlavourEn ",
                " Organic ",
                " MilkTypeEn ",
                " MilkTreatmentTypeEn ");

        CheeseSchema schema = CheeseSchema.fromHeader(header);

        assertEquals(0, schema.cheeseIdIndex());
        assertEquals(1, schema.moisturePercentIndex());
        assertEquals(2, schema.flavourIndex());
        assertEquals(3, schema.organicIndex());
        assertEquals(4, schema.milkTypeIndex());
        assertEquals(5, schema.milkTreatmentTypeIndex());
    }

    @Test
    void validatesRecordWithExpectedFieldCount() {
        CheeseSchema schema = CheeseSchema.fromHeader(actualHeader());

        schema.validateFieldCount(actualHeader());
    }

    @Test
    void rejectsRecordWithTooFewFields() {
        CheeseSchema schema = CheeseSchema.fromHeader(actualHeader());
        List<String> fields = new ArrayList<>(actualHeader());
        fields.removeLast();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schema.validateFieldCount(fields));

        assertEquals("Expected 13 columns but found 12.", exception.getMessage());
    }

    @Test
    void rejectsRecordWithTooManyFields() {
        CheeseSchema schema = CheeseSchema.fromHeader(actualHeader());
        List<String> fields = new ArrayList<>(actualHeader());
        fields.add("Unexpected");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schema.validateFieldCount(fields));

        assertEquals("Expected 13 columns but found 14.", exception.getMessage());
    }

    @Test
    void rejectsEachMissingRequiredHeader() {
        List<String> requiredHeaders = List.of(
                "CheeseId",
                "MoisturePercent",
                "FlavourEn",
                "Organic",
                "MilkTypeEn",
                "MilkTreatmentTypeEn");

        for (String missingHeader : requiredHeaders) {
            List<String> header = new ArrayList<>(actualHeader());
            header.remove(missingHeader);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> CheeseSchema.fromHeader(header));
        }
    }

    @Test
    void rejectsDuplicateHeader() {
        List<String> header = new ArrayList<>(actualHeader());
        header.add("CheeseId");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> CheeseSchema.fromHeader(header));

        assertEquals("Duplicate CSV header: CheeseId.", exception.getMessage());
    }

    @Test
    void rejectsBlankHeader() {
        List<String> header = new ArrayList<>(actualHeader());
        header.set(2, "   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> CheeseSchema.fromHeader(header));

        assertEquals("Header field at column 3 is blank.", exception.getMessage());
    }

    @Test
    void rejectsEmptyHeaderList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CheeseSchema.fromHeader(List.of()));
    }

    @Test
    void rejectsNullHeaderList() {
        assertThrows(
                NullPointerException.class,
                () -> CheeseSchema.fromHeader(null));
    }

    @Test
    void rejectsNullHeaderField() {
        List<String> header = new ArrayList<>(actualHeader());
        header.set(4, null);

        assertThrows(
                NullPointerException.class,
                () -> CheeseSchema.fromHeader(header));
    }

    @Test
    void validateFieldCountRejectsNullList() {
        CheeseSchema schema = CheeseSchema.fromHeader(actualHeader());

        assertThrows(
                NullPointerException.class,
                () -> schema.validateFieldCount(null));
    }

    @Test
    void constructorRejectsInvalidColumnCountAndIndexes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CheeseSchema(0, 0, 0, 0, 0, 0, 0));

        assertThrows(
                IllegalArgumentException.class,
                () -> new CheeseSchema(6, -1, 1, 2, 3, 4, 5));

        assertThrows(
                IllegalArgumentException.class,
                () -> new CheeseSchema(6, 0, 6, 2, 3, 4, 5));
    }

    private static List<String> actualHeader() {
        return Arrays.asList(
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
}
