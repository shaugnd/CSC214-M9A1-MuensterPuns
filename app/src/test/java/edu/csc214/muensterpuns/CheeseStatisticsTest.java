package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

/**
 * Tests one-pass statistical accumulation in {@link CheeseStatistics}.
 *
 * <p>The tests cover treatment counts, moisture calculations, organic
 * thresholds, combined milk sources, lactic-flavour matching, ID tracking,
 * duplicate detection, and defensive copying.</p>
 */
class CheeseStatisticsTest {
    // This is really more of a mental exercise in designing tests than anything else.
    // I think I have covered everything, but let me know if I have missed something.
    @Test
    void emptyAccumulatorProducesZeroedReport() {
        CheeseStatistics statistics = new CheeseStatistics();

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(0, report.cheesesAnalyzed());
        assertEquals(0, report.pasteurizedCount());
        assertEquals(0, report.rawMilkCount());
        assertEquals(0, report.organicHighMoistureCount());
        assertEquals("", report.mostCommonMilkSource());
        assertEquals(0, report.mostCommonMilkSourceCount());
        assertEquals(0, report.validMoistureCount());
        assertEquals(0.0, report.averageMoisturePercent());
        assertEquals(0, report.lacticFlavourCount());
        assertEquals(0, statistics.getMaximumId());
        assertTrue(statistics.getPresentIds().isEmpty());
    }

    @Test
    void countsOnePasteurizedCheese() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(228, 47.0, false, "Cow", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(1, report.cheesesAnalyzed());
        assertEquals(1, report.pasteurizedCount());
        assertEquals(0, report.rawMilkCount());
    }

    @Test
    void countsOneRawMilkCheese() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(229, 48.0, false, "Goat", "Raw Milk", "Sharp"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(0, report.pasteurizedCount());
        assertEquals(1, report.rawMilkCount());
    }

    @Test
    void milkTreatmentMatchingIsCaseInsensitive() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(230, 45.0, false, "Cow", "PASTEURIZED", "Mild"));
        statistics.accept(cheese(231, 46.0, false, "Cow", "raw milk", "Sharp"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(1, report.pasteurizedCount());
        assertEquals(1, report.rawMilkCount());
    }

    @Test
    void unknownAndBlankTreatmentsAreIgnored() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(232, 45.0, false, "Cow", "", "Mild"));
        statistics.accept(cheese(233, 46.0, false, "Cow", "Thermised", "Sharp"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(0, report.pasteurizedCount());
        assertEquals(0, report.rawMilkCount());
        assertEquals(2, report.cheesesAnalyzed());
    }

    @Test
    void calculatesAverageFromAvailableMoistureValues() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(234, 40.0, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(235, 50.0, false, "Goat", "Pasteurized", "Sharp"));
        statistics.accept(cheese(236, 60.0, false, "Ewe", "Raw Milk", "Rich"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(3, report.validMoistureCount());
        assertEquals(50.0, report.averageMoisturePercent(), 0.000001);
    }

    @Test
    void missingMoistureDoesNotAffectAverage() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(237, 40.0, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(238, null, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(239, 60.0, false, "Cow", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(2, report.validMoistureCount());
        assertEquals(50.0, report.averageMoisturePercent(), 0.000001);
    }

    @Test
    void countsOrganicCheeseOnlyWhenMoistureIsAboveThreshold() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(240, 41.0, true, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(241, 41.1, true, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(242, 55.0, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(243, null, true, "Cow", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(1, report.organicHighMoistureCount());
    }

    @Test
    void countsSingleMilkSources() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(244, 45.0, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(245, 45.0, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(246, 45.0, false, "Goat", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals("Cow", report.mostCommonMilkSource());
        assertEquals(2, report.mostCommonMilkSourceCount());
    }

    @Test
    void combinedMilkTypesCreditEveryNamedAnimal() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(247, 45.0, false, "Cow, Goat and Ewe", "Pasteurized", "Mild"));
        statistics.accept(cheese(248, 45.0, false, "Goat", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals("Goat", report.mostCommonMilkSource());
        assertEquals(2, report.mostCommonMilkSourceCount());
    }

    @Test
    void buffaloCowIsCountedAsBuffaloOnly() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(249, 45.0, false, "Buffalo Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(250, 45.0, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(251, 45.0, false, "Buffalo", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals("Buffalo", report.mostCommonMilkSource());
        assertEquals(2, report.mostCommonMilkSourceCount());
    }

    @Test
    void blankAndUnknownMilkSourcesAreIgnored() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(252, 45.0, false, "", "Pasteurized", "Mild"));
        statistics.accept(cheese(253, 45.0, false, "Yak", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals("", report.mostCommonMilkSource());
        assertEquals(0, report.mostCommonMilkSourceCount());
    }

    @Test
    void tiedMilkSourceCountsUseStableConfiguredOrder() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(254, 45.0, false, "Goat", "Pasteurized", "Mild"));
        statistics.accept(cheese(255, 45.0, false, "Cow", "Pasteurized", "Mild"));

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals("Cow", report.mostCommonMilkSource());
        assertEquals(1, report.mostCommonMilkSourceCount());
    }

    @Test
    void countsWholeWordLacticCaseInsensitively() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(256, 45.0, false, "Cow", "Pasteurized", "Lactic and fruity"));
        statistics.accept(cheese(257, 45.0, false, "Cow", "Pasteurized", "Sharp, LACTIC finish"));
        statistics.accept(cheese(258, 45.0, false, "Cow", "Pasteurized", "Mild and lactic."));

        assertEquals(3, statistics.createReport().lacticFlavourCount());
    }

    @Test
    void doesNotCountLacticLettersInsideLongerWord() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(259, 45.0, false, "Cow", "Pasteurized", "Galactic flavour"));

        assertEquals(0, statistics.createReport().lacticFlavourCount());
    }

    @Test
    void blankFlavourIsNotCountedAsLactic() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(260, 45.0, false, "Cow", "Pasteurized", ""));

        assertEquals(0, statistics.createReport().lacticFlavourCount());
    }

    @Test
    void tracksObservedIdsAndMaximumId() {
        CheeseStatistics statistics = new CheeseStatistics();

        statistics.accept(cheese(228, 45.0, false, "Cow", "Pasteurized", "Mild"));
        statistics.accept(cheese(900, 46.0, false, "Goat", "Raw Milk", "Sharp"));
        statistics.accept(cheese(2391, 47.0, false, "Ewe", "Pasteurized", "Rich"));

        BitSet presentIds = statistics.getPresentIds();

        assertTrue(presentIds.get(228));
        assertTrue(presentIds.get(900));
        assertTrue(presentIds.get(2391));
        assertFalse(presentIds.get(229));
        assertEquals(2391, statistics.getMaximumId());
    }

    @Test
    void returnedIdSetIsDefensiveCopy() {
        CheeseStatistics statistics = new CheeseStatistics();
        statistics.accept(cheese(228, 45.0, false, "Cow", "Pasteurized", "Mild"));

        BitSet copy = statistics.getPresentIds();
        copy.clear(228);
        copy.set(999);

        BitSet secondCopy = statistics.getPresentIds();

        assertTrue(secondCopy.get(228));
        assertFalse(secondCopy.get(999));
    }

    @Test
    void rejectsDuplicateCheeseId() {
        CheeseStatistics statistics = new CheeseStatistics();
        statistics.accept(cheese(228, 45.0, false, "Cow", "Pasteurized", "Mild"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> statistics.accept(cheese(228, 50.0, true, "Goat", "Raw Milk", "Sharp")));

        assertEquals("Duplicate cheese ID 228.", exception.getMessage());
    }

    @Test
    void rejectsNullCheese() {
        CheeseStatistics statistics = new CheeseStatistics();

        assertThrows(NullPointerException.class, () -> statistics.accept(null));
    }

    @Test
    void reportReflectsAllAcceptedRecordsWithoutRetainingThem() {
        CheeseStatistics statistics = new CheeseStatistics();

        for (int id = 300; id < 400; id++) {
            statistics.accept(cheese(id, 50.0, false, "Cow", "Pasteurized", "Mild"));
        }

        CheeseStatisticsReport report = statistics.createReport();

        assertEquals(100, report.cheesesAnalyzed());
        assertEquals(100, report.pasteurizedCount());
        assertEquals(100, report.validMoistureCount());
        assertEquals(50.0, report.averageMoisturePercent(), 0.000001);
        assertEquals(100, report.mostCommonMilkSourceCount());
    }

    private static Cheese cheese(
            int id,
            Double moisture,
            Boolean organic,
            String milkType,
            String treatment,
            String flavour) {

        return new Cheese(id, moisture, flavour, organic, milkType, treatment);
    }
}
