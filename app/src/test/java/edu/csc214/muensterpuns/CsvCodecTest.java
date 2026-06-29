package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests parsing and serialization behavior in {@link CsvCodec}.
 *
 * <p>The tests cover empty values, quoted delimiters, escaped quotation marks,
 * Unicode text, round-trip preservation, malformed input, and null arguments.</p>
 */
class CsvCodecTest {
    // The CSV Codec underpins everything in the program, so I'm going to test
    // the heck out of it.
    private final CsvCodec codec = new CsvCodec();

    // Zero

    @Test
    void parsesEmptyRecordAsOneEmptyField() {
        assertEquals(List.of(""), codec.parse(""));
    }

    @Test
    void parsesSeveralEmptyFields() {
        assertEquals(List.of("", "", ""), codec.parse(",,"));
    }

    @Test
    void formatsSeveralEmptyFields() {
        assertEquals(",,", codec.format(List.of("", "", "")));
    }

    // One

    @Test
    void parsesOneUnquotedField() {
        assertEquals(List.of("Cheddar"), codec.parse("Cheddar"));
    }

    @Test
    void parsesOneQuotedField() {
        assertEquals(List.of("Cheddar"), codec.parse("\"Cheddar\""));
    }

    @Test
    void parsesQuotedEmptyField() {
        assertEquals(List.of(""), codec.parse("\"\""));
    }

    // Many

    @Test
    void parsesOrdinaryCsvRecord() {
        String record = "228,NB,Farmstead,47.0,Mild,Creamy,0,Semi-soft,Cow,Pasteurized,Washed,Sample Cheese,lower fat";

        assertEquals(List.of(
                "228",
                "NB",
                "Farmstead",
                "47.0",
                "Mild",
                "Creamy",
                "0",
                "Semi-soft",
                "Cow",
                "Pasteurized",
                "Washed",
                "Sample Cheese",
                "lower fat"), codec.parse(record));
    }

    @Test
    void parsesCommaInsideQuotedField() {
        assertEquals(List.of("228", "Rich, creamy, buttery and mild", "Cow"), codec.parse("228,\"Rich, creamy, buttery and mild\",Cow"));
    }

    @Test
    void parsesEscapedQuotationMarks() {
        assertEquals(List.of("Say \"cheese\"", "Cow"), codec.parse("\"Say \"\"cheese\"\"\",Cow"));
    }

    @Test
    void preservesUnicodeCharacters() {
        assertEquals(List.of("Québec", "Crème fraîche", "Chèvre"), codec.parse("Québec,Crème fraîche,Chèvre"));
    }

    @Test
    void parsesTrailingEmptyField() {
        assertEquals(List.of("228", "Cow", ""), codec.parse("228,Cow,"));
    }

    @Test
    void formatsFieldsThatContainCommasAndQuotationMarks() {
        List<String> fields = List.of("228", "Rich, creamy", "Say \"cheese\"", "Cow");

        assertEquals("228,\"Rich, creamy\",\"Say \"\"cheese\"\"\",Cow", codec.format(fields));
    }

    @Test
    void formatsLineBreakCharactersInsideQuotedField() {
        List<String> fields = List.of("first line\nsecond line", "Cow");

        assertEquals("\"first line\nsecond line\",Cow", codec.format(fields));
    }

    @Test
    void parseFormatParseRoundTripPreservesFields() {
        List<String> originalFields = List.of(
                "228",
                "Québec",
                "",
                "Rich, creamy and \"lactic\"",
                "Cow and Goat",
                "Cheese\nName");

        String formattedRecord = codec.format(originalFields);

        assertEquals(originalFields, codec.parse(formattedRecord));
    }

    // Boundary

    @Test
    void parsesQuotedFieldAtBeginningAndEndOfRecord() {
        assertEquals(List.of("first, field", "middle", "last, field"), codec.parse("\"first, field\",middle,\"last, field\""));
    }

    // Exception

    @Test
    void rejectsUnclosedQuotedField() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> codec.parse("228,\"Unclosed field,Cow"));

        assertEquals("Unclosed quoted field.", exception.getMessage());
    }

    @Test
    void rejectsQuotationMarkInsideUnquotedField() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> codec.parse("228,Chee\"se,Cow"));

        assertEquals("Unexpected quotation mark at character 9.", exception.getMessage());
    }

    @Test
    void rejectsCharactersAfterClosingQuotationMark() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> codec.parse("\"Cheese\"unexpected,Cow"));

        assertEquals("Unexpected character after closing quotation mark at character 9.", exception.getMessage());
    }

    @Test
    void rejectsNullRecord() {
        assertThrows(NullPointerException.class, () -> codec.parse(null));
    }

    @Test
    void rejectsNullFieldList() {
        assertThrows(NullPointerException.class, () -> codec.format(null));
    }

    @Test
    void rejectsNullFieldWithinList() {
        assertThrows(NullPointerException.class, () -> codec.format(java.util.Arrays.asList("228", null, "Cow")));
    }
}
