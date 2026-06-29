package edu.csc214.muensterpuns;

import java.util.Objects;

/**
 * Represents the analysis-relevant fields from one cheese record.
 *
 * <p>The original CSV row does not need to remain in memory after this object
 * has been analyzed and any transformed output rows have been written.</p>
 */
public record Cheese(
        int id,
        Double moisturePercent,
        String flavour,
        Boolean organic,
        String milkType,
        String milkTreatmentType) {
    // I've chosen a streaming architecture to keep the resource utilization
    // from scaling significantly with larger files.  This class basically 
    // just holds the elements of the row that are relevant for this program.
    // Once I've accumulated the statistics for this row, it can be binned.
    public Cheese {
        if (id <= 0) {
            throw new IllegalArgumentException("Cheese ID must be positive.");
        }

        if (moisturePercent != null
                && (!Double.isFinite(moisturePercent)
                || moisturePercent < 0.0
                || moisturePercent > 100.0)) {
            throw new IllegalArgumentException(
                    "Moisture percentage must be between 0 and 100.");
        }

        flavour = Objects.requireNonNull(
                flavour,
                "Flavour cannot be null.").strip();

        milkType = Objects.requireNonNull(
                milkType,
                "Milk type cannot be null.").strip();

        milkTreatmentType = Objects.requireNonNull(
                milkTreatmentType,
                "Milk treatment type cannot be null.").strip();
    }
}