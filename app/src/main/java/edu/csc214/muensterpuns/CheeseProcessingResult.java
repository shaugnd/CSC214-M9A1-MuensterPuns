package edu.csc214.muensterpuns;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Contains the completed results of one cheese-data processing run.
 *
 * <p>The result combines the statistical report with generated-file locations
 * and counts that are useful for console confirmation.</p>
 */
public record CheeseProcessingResult(
        CheeseStatisticsReport statisticsReport,
        int malformedRowCount,
        int missingIdCount,
        Path reportPath,
        Path withoutHeadersPath,
        Path withoutIdsPath,
        Path missingIdsPath) {
            
    // This is an immutable record that simply serves to carry the final stats, malformed-row count
    // missing-ID count, and generated output paths back to the driver app.  That's it.

    public CheeseProcessingResult {
        statisticsReport = Objects.requireNonNull(
                statisticsReport,
                "Statistics report cannot be null.");

        reportPath = Objects.requireNonNull(
                reportPath,
                "Report path cannot be null.");

        withoutHeadersPath = Objects.requireNonNull(
                withoutHeadersPath,
                "Headerless CSV path cannot be null.");

        withoutIdsPath = Objects.requireNonNull(
                withoutIdsPath,
                "ID-free CSV path cannot be null.");

        missingIdsPath = Objects.requireNonNull(
                missingIdsPath,
                "Missing-ID path cannot be null.");

        if (malformedRowCount < 0) {
            throw new IllegalArgumentException(
                    "Malformed-row count cannot be negative.");
        }

        if (missingIdCount < 0) {
            throw new IllegalArgumentException(
                    "Missing-ID count cannot be negative.");
        }
    }
}