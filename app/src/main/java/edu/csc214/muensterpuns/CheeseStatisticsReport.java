package edu.csc214.muensterpuns;

import java.util.Objects;

/**
 * Contains the completed statistical results from the cheese dataset.
 *
 * <p>The report is immutable so output components can safely format the
 * results without accessing or modifying the accumulator's internal state.</p>
 */
public record CheeseStatisticsReport(
        int cheesesAnalyzed,
        int pasteurizedCount,
        int rawMilkCount,
        int organicHighMoistureCount,
        String mostCommonMilkSource,
        int mostCommonMilkSourceCount,
        int validMoistureCount,
        double averageMoisturePercent,
        int lacticFlavourCount) {

    // For now, I'm just putting in the whole report getter.  It might make sense
    // to have getters for each element of the report if we develop an interactive
    // version of this app, but for now we will just leave the class as is knowing
    // that this is where accessor code needs to go if we get functional expansion.
    //
    // Some assumptions here.  When no valid moisture values exist, the report will
    // use 0.0 as the average.  When no milk-source values exist, it will use an
    // empty source name and a count of zero.

    public CheeseStatisticsReport {
        requireNonNegative(cheesesAnalyzed, "Cheeses analyzed");
        requireNonNegative(pasteurizedCount, "Pasteurized count");
        requireNonNegative(rawMilkCount, "Raw-milk count");
        requireNonNegative(organicHighMoistureCount, "Organic high-moisture count");
        requireNonNegative(mostCommonMilkSourceCount, "Most-common milk-source count");
        requireNonNegative(validMoistureCount, "Valid moisture count");
        requireNonNegative(lacticFlavourCount, "Lactic flavour count");

        mostCommonMilkSource = Objects.requireNonNull(
                mostCommonMilkSource,
                "Most-common milk source cannot be null.").strip();

        if (!Double.isFinite(averageMoisturePercent)
                || averageMoisturePercent < 0.0
                || averageMoisturePercent > 100.0) {
            throw new IllegalArgumentException(
                    "Average moisture percentage must be between 0 and 100.");
        }

        if (pasteurizedCount > cheesesAnalyzed
                || rawMilkCount > cheesesAnalyzed
                || organicHighMoistureCount > cheesesAnalyzed
                || mostCommonMilkSourceCount > cheesesAnalyzed
                || validMoistureCount > cheesesAnalyzed
                || lacticFlavourCount > cheesesAnalyzed) {
            throw new IllegalArgumentException(
                    "A statistical count cannot exceed the number of cheeses analyzed.");
        }

        if (validMoistureCount == 0 && averageMoisturePercent != 0.0) {
            throw new IllegalArgumentException(
                    "Average moisture must be zero when no moisture values are available.");
        }

        if (mostCommonMilkSource.isEmpty() && mostCommonMilkSourceCount != 0) {
            throw new IllegalArgumentException(
                    "A milk-source count requires a milk-source name.");
        }
    }

    private static void requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
    }
}