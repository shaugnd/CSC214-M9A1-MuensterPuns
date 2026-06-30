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
 * Tests conversion of schema-aligned CSV fields into usable cheese records and
 * nonfatal field diagnostics.
 */
class CheeseParserTest {
    private final CheeseParser parser = new CheeseParser();
    private final CheeseSchema schema = CheeseSchema.fromHeader(header());

    @Test
    void parsesValidRecord() {
        CheeseParseResult result = parser.parse(validFields(), schema);
        Cheese cheese = result.cheese();

        assertEquals(228, cheese.id());
        assertEquals(47.5, cheese.moisturePercent());
        assertEquals("Mild, lactic flavour", cheese.flavour());
        assertEquals(Boolean.TRUE, cheese.organic());
        assertEquals("Cow", cheese.milkType());
        assertEquals("Pasteurized", cheese.milkTreatmentType());
        assertFalse(result.hasProblems());
        assertEquals(List.of(), result.problems());
    }

    @Test
    void stripsValuesWhenCheeseRecordIsCreated() {
        List<String> fields = validFields();
        fields.set(0, " 228 ");
        fields.set(3, " 47.5 ");
        fields.set(4, "  Mild flavour  ");
        fields.set(6, " 1 ");
        fields.set(8, "  Cow  ");
        fields.set(9, "  Pasteurized  ");

        Cheese cheese = parser.parse(fields, schema).cheese();

        assertEquals(228, cheese.id());
        assertEquals(47.5, cheese.moisturePercent());
        assertEquals("Mild flavour", cheese.flavour());
        assertEquals(Boolean.TRUE, cheese.organic());
        assertEquals("Cow", cheese.milkType());
        assertEquals("Pasteurized", cheese.milkTreatmentType());
    }

    @Test
    void blankOptionalValuesBecomeNullOrBlank() {
        List<String> fields = validFields();
        fields.set(3, "");
        fields.set(4, "");
        fields.set(6, "");
        fields.set(8, "");
        fields.set(9, "");

        CheeseParseResult result = parser.parse(fields, schema);
        Cheese cheese = result.cheese();

        assertNull(cheese.moisturePercent());
        assertEquals("", cheese.flavour());
        assertNull(cheese.organic());
        assertEquals("", cheese.milkType());
        assertEquals("", cheese.milkTreatmentType());
        assertFalse(result.hasProblems());
    }

    @Test
    void zeroOrganicValueBecomesFalse() {
        List<String> fields = validFields();
        fields.set(6, "0");

        CheeseParseResult result = parser.parse(fields, schema);

        assertEquals(Boolean.FALSE, result.cheese().organic());
        assertFalse(result.hasProblems());
    }

    @Test
    void invalidMoistureIsNonfatal() {
        List<String> fields = validFields();
        fields.set(3, "forty-seven");

        CheeseParseResult result = parser.parse(fields, schema);

        assertNull(result.cheese().moisturePercent());
        assertEquals("Pasteurized", result.cheese().milkTreatmentType());
        assertTrue(result.hasProblems());
        assertEquals(
                List.of("Invalid moisture value \"forty-seven\"."),
                result.problems());
    }

    @Test
    void moistureBelowZeroIsNonfatal() {
        List<String> fields = validFields();
        fields.set(3, "-0.1");

        CheeseParseResult result = parser.parse(fields, schema);

        assertNull(result.cheese().moisturePercent());
        assertEquals(
                List.of("Moisture percentage must be between 0 and 100."),
                result.problems());
    }

    @Test
    void moistureAboveOneHundredIsNonfatal() {
        List<String> fields = validFields();
        fields.set(3, "100.1");

        CheeseParseResult result = parser.parse(fields, schema);

        assertNull(result.cheese().moisturePercent());
        assertEquals(
                List.of("Moisture percentage must be between 0 and 100."),
                result.problems());
    }

    @Test
    void nonFiniteMoistureIsNonfatal() {
        List<String> fields = validFields();
        fields.set(3, "NaN");

        CheeseParseResult result = parser.parse(fields, schema);

        assertNull(result.cheese().moisturePercent());
        assertEquals(
                List.of("Moisture percentage must be between 0 and 100."),
                result.problems());
    }

    @Test
    void moistureBoundariesAreValid() {
        List<String> zeroFields = validFields();
        zeroFields.set(3, "0");

        List<String> hundredFields = validFields();
        hundredFields.set(3, "100");

        assertEquals(0.0, parser.parse(zeroFields, schema).cheese().moisturePercent());
        assertEquals(100.0, parser.parse(hundredFields, schema).cheese().moisturePercent());
    }

    @Test
    void invalidOrganicValueIsNonfatal() {
        List<String> fields = validFields();
        fields.set(6, "yes");

        CheeseParseResult result = parser.parse(fields, schema);

        assertNull(result.cheese().organic());
        assertEquals(47.5, result.cheese().moisturePercent());
        assertEquals(
                List.of("Invalid organic value \"yes\". Expected 0 or 1."),
                result.problems());
    }

    @Test
    void collectsSeveralNonfatalProblems() {
        List<String> fields = validFields();
        fields.set(3, "unknown");
        fields.set(6, "organic");

        CheeseParseResult result = parser.parse(fields, schema);

        assertNull(result.cheese().moisturePercent());
        assertNull(result.cheese().organic());
        assertEquals(
                List.of(
                        "Invalid moisture value \"unknown\".",
                        "Invalid organic value \"organic\". Expected 0 or 1."),
                result.problems());
    }

    @Test
    void blankIdIsFatalAndUnavailable() {
        List<String> fields = validFields();
        fields.set(0, "   ");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("unavailable", exception.recordId());
        assertEquals("Cheese ID is blank.", exception.getMessage());
    }

    @Test
    void nonnumericIdIsFatalAndRecoverable() {
        List<String> fields = validFields();
        fields.set(0, "ABC");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("ABC", exception.recordId());
        assertEquals("Invalid cheese ID \"ABC\".", exception.getMessage());
    }

    @Test
    void zeroIdIsFatal() {
        List<String> fields = validFields();
        fields.set(0, "0");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("0", exception.recordId());
        assertEquals("Cheese ID must be positive.", exception.getMessage());
    }

    @Test
    void negativeIdIsFatal() {
        List<String> fields = validFields();
        fields.set(0, "-5");

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("-5", exception.recordId());
        assertEquals("Cheese ID must be positive.", exception.getMessage());
    }

    @Test
    void incorrectFieldCountIsFatal() {
        List<String> fields = new ArrayList<>(validFields());
        fields.remove(fields.size() - 1);

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
    }

    @Test
    void nullRequiredTextFieldIsFatal() {
        List<String> fields = validFields();
        fields.set(4, null);

        MalformedCheeseException exception = assertThrows(
                MalformedCheeseException.class,
                () -> parser.parse(fields, schema));

        assertEquals("228", exception.recordId());
        assertEquals("FlavourEn cannot be null.", exception.getMessage());
    }

    @Test
    void rejectsNullFieldList() {
        assertThrows(
                NullPointerException.class,
                () -> parser.parse(null, schema));
    }

    @Test
    void rejectsNullSchema() {
        assertThrows(
                NullPointerException.class,
                () -> parser.parse(validFields(), null));
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
                "47.5",
                "Mild, lactic flavour",
                "Soft",
                "1",
                "Firm Cheese",
                "Cow",
                "Pasteurized",
                "Washed Rind",
                "Test Cheese",
                "Lower Fat"));
    }
}
