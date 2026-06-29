package edu.csc214.muensterpuns;

import java.util.Objects;

/**
 * Describes a malformed cheese record while preserving any recoverable ID.
 *
 * <p>The file processor adds the original CSV line number when reporting the
 * problem to the console.</p>
 */
public final class MalformedCheeseException extends IllegalArgumentException {
    // We learned how to create custom exceptions.  Let's leverage that here
    // just to exercise the skill.  Probably not strictly necessary, but it
    // feels good to put this in place.
    private final String recordId;

    public MalformedCheeseException(String recordId, String message) {
        super(Objects.requireNonNull(message, "Error message cannot be null."));

        String normalizedId = recordId == null ? "" : recordId.strip();
        this.recordId = normalizedId.isEmpty() ? "unavailable" : normalizedId;
    }

    public String recordId() {
        return recordId;
    }
}
