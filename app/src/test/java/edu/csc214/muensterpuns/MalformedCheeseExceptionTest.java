package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests ID normalization and message preservation in
 * {@link MalformedCheeseException}.
 */
class MalformedCheeseExceptionTest {
    // We are here and coding.  Why not a couple of tests for the exception.
    @Test
    void preservesSuppliedRecordIdAndMessage() {
        MalformedCheeseException exception = new MalformedCheeseException("1038", "Invalid moisture value.");

        assertEquals("1038", exception.recordId());
        assertEquals("Invalid moisture value.", exception.getMessage());
    }

    @Test
    void stripsWhitespaceFromRecordId() {
        MalformedCheeseException exception = new MalformedCheeseException("  1038  ", "Invalid record.");

        assertEquals("1038", exception.recordId());
    }

    @Test
    void blankRecordIdBecomesUnavailable() {
        MalformedCheeseException exception = new MalformedCheeseException("   ", "Cheese ID is blank.");

        assertEquals("unavailable", exception.recordId());
    }

    @Test
    void nullRecordIdBecomesUnavailable() {
        MalformedCheeseException exception = new MalformedCheeseException(null, "Cheese ID is unavailable.");

        assertEquals("unavailable", exception.recordId());
    }

    @Test
    void nullMessageIsRejected() {
        assertThrows(NullPointerException.class, () -> new MalformedCheeseException("1038", null));
    }
}