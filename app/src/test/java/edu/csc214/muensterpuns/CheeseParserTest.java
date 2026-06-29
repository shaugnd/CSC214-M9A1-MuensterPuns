package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests schema-aware conversion of CSV fields into {@link Cheese} records.
 *
 * <p>The tests cover valid rows, missing optional values, reordered columns,
 * malformed numeric and Boolean fields, record-width problems, and null
 * dependencies.</p>
 */
class CheeseParserTest {
    private final CheeseParser parser = new CheeseParser();
    private final CheeseSchema schema = CheeseSchema.fromHeader(header());

    @Test
    void parsesCompleteValidRecord() {
        Cheese cheese = parser.parse(validFields(), schema);

        assertEquals(228, cheese.id());
        assertEquals(47.0, cheese.moisturePercent());
        assertEquals("Mild and lactic", cheese.flavour());
        assertFalse(cheese.organic());
        assertEquals("Cow", cheese.milkType());
        assertEquals("Pasteurized", cheese.milkTreatmentType());
    }

    @Test
    void parsesOrganicCheese() {
        List<String> fields = validFields();
        fields.set(schema.organicIndex(), "1");

        Cheese cheese = parser.parse(fields, schema);

        assertTrue(cheese.organic());
    }

    @Test
    void missingMoistureBecomesNull() {
        List<String> fields = validFields();
        fields.set(schema.moisturePercentIndex(), "");

        Cheese cheese = parser.parse(fields, schema);

        assertNull(cheese.moisturePercent());
    }

    @Test
    void missingOrganicValueBecomesNull() {
        List<String> fields = validFields();
        fields.set(schema.organicIndex(), "   ");

        Cheese cheese = parser.parse(fields, schema);

        assertNull(cheese.organic());
    }

    @Test
    void blankTextFieldsRemainEmptyStrings() {
        List<String> fields = validFields();
        fields.set(schema.flavourIndex(), "");
        fields.set(schema.milkTypeIndex(), "   ");
        fields.set(schema.milkTreatmentTypeIndex(), "");

        Cheese cheese = parser.parse(fields, schema);

        assertEquals("", cheese.flavour());
        assertEquals("", cheese.milkType());
        assertEquals("", cheese.milkTreatmentType());
    }

    @Test
    void trimsAnalysisTextFields() {
        List<String> fields = validFields();
        fields.set(schema.flavourIndex(), "  Mild and lactic  ");
        fields.set(schema.milkTypeIndex(), " Cow ");
        fields.set(schema.milkTreatmentTypeIndex(), " Pasteurized ");

        Cheese cheese = parser.parse(fields, schema);

        assertEquals("Mild and lactic", cheese.flavour());
        assertEquals("Cow", cheese.milkType());
        assertEquals("Pasteurized", cheese.milkTreatmentType());
    }

    @Test
    void parsesFieldsUsingReorderedSchema() {
        CheeseSchema reorderedSchema = CheeseSchema.fromHeader(List.of(
                "Organic",
                "FlavourEn",
                "MilkTreatmentTypeEn",
                "CheeseId",
                "MilkTypeEn",
                "MoisturePercent"));

        List<String> fields = List.of(
                "1",
                "Lactic and sharp",
                "Raw Milk",
                "900",
                "Goat",
                "52.5");

        Cheese cheese = parser.parse(fields, reorderedSchema);

        assertEquals(900, cheese.id());
        assertEquals(52.5, cheese.moisturePercent());
        assertEquals("Lactic and sharp", cheese.flavour());
        assertTrue(cheese.organic());
        assertEquals("Goat", cheese.milkType());
        assertEquals("Raw Milk", cheese.milkTreatmentType());
    }

    @Test
    void rejectsBlankCheeseId() {
        List<String> fields = validFields();
        fields.set(schema.cheeseIdIndex(), " ");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("unavailable", exception.recordId());
        assertEquals("Cheese ID is blank.", exception.getMessage());
    }

    @Test
    void rejectsNonIntegerCheeseId() {
        List<String> fields = validFields();
        fields.set(schema.cheeseIdIndex(), "ABC");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("ABC", exception.recordId());
        assertEquals("Invalid cheese ID \"ABC\".", exception.getMessage());
    }

    @Test
    void rejectsNonPositiveCheeseId() {
        List<String> fields = validFields();
        fields.set(schema.cheeseIdIndex(), "0");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("0", exception.recordId());
        assertEquals("Cheese ID must be positive.", exception.getMessage());
    }

    @Test
    void rejectsInvalidMoistureText() {
        List<String> fields = validFields();
        fields.set(schema.moisturePercentIndex(), "forty-seven");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
        assertEquals("Invalid moisture value \"forty-seven\".", exception.getMessage());
    }

    @Test
    void rejectsMoistureOutsideValidRange() {
        List<String> fields = validFields();
        fields.set(schema.moisturePercentIndex(), "101.0");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
        assertEquals("Moisture percentage must be between 0 and 100.", exception.getMessage());
    }

    @Test
    void rejectsNonFiniteMoisture() {
        List<String> fields = validFields();
        fields.set(schema.moisturePercentIndex(), "NaN");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("Moisture percentage must be between 0 and 100.", exception.getMessage());
    }

    @Test
    void rejectsInvalidOrganicValue() {
        List<String> fields = validFields();
        fields.set(schema.organicIndex(), "yes");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
        assertEquals("Invalid organic value \"yes\". Expected 0 or 1.", exception.getMessage());
    }

    @Test
    void reportsTooFewColumnsWithRecoverableId() {
        List<String> fields = validFields();
        fields.removeLast();

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
        assertEquals("Expected 13 columns but found 12.", exception.getMessage());
    }

    @Test
    void reportsTooManyColumnsWithRecoverableId() {
        List<String> fields = validFields();
        fields.add("Unexpected");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
        assertEquals("Expected 13 columns but found 14.", exception.getMessage());
    }

    @Test
    void nullRequiredFieldProducesMalformedRecord() {
        List<String> fields = validFields();
        fields.set(schema.flavourIndex(), null);

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
        assertEquals("FlavourEn cannot be null.", exception.getMessage());
    }

    @Test
    void nullCheeseIdFieldMakesIdUnavailable() {
        List<String> fields = validFields();
        fields.set(schema.cheeseIdIndex(), null);

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("unavailable", exception.recordId());
        assertEquals("CheeseId cannot be null.", exception.getMessage());
    }

    @Test
    void rejectsNullFieldList() {
        assertThrows(NullPointerException.class, () -> parser.parse(null, schema));
    }

    @Test
    void rejectsNullSchema() {
        assertThrows(NullPointerException.class, () -> parser.parse(validFields(), null));
    }

    private static List<String> header() {
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

    private static List<String> validFields() {
        return new ArrayList<>(List.of(
                "228",
                "NB",
                "Farmstead",
                "47.0",
                "Mild and lactic",
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
