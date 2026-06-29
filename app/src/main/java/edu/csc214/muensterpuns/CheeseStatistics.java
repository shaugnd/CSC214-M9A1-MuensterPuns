package edu.csc214.muensterpuns;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Accumulates cheese statistics while records are streamed through the
 * application.
 *
 * <p>The accumulator does not retain the cheese records themselves. It stores
 * only running totals, milk-source counts, and a compact set of observed IDs.</p>
 */
public final class CheeseStatistics {
    // For memory efficiency, we do this in a single pass and accumulate the summary
    // results taht we need to report instead of reading the whole file and then trying
    // to do soemthing with that data.  This insulates us from having resource issues if
    // given a file with a zillion rows in it.  Again, probably overkill here, but I am 
    // using a BitSet here for the ID tracking because it is efficient and super quick, 
    // especiallygiven that there are a lot of IDs missing.
    //
    // If we are really wanting to insulate from a zillion row file, then we might actually
    // do better to use a HashSet of int since the BitSet is as big as the number of rows in
    // our file.  For this project, I made the call to stick with a BitSet.
    //
    // From a business rules perspective, I decided to accumulate for every reference to an 
    // animal, so if a chees has Cow, Goat, and Ewe milk, then each of those get incremented
    // consequently the total animal count will NOT equal the total record count.  The one 
    // exception is Buffalo Cow.  That is treated as Buffalo only since "cow" is describing
    // the female of Buffalo in this case.  The descriptor is kind of redundant because you 
    // are not going to get milk from a male buffalo, but the data is what it is.

    private static final double HIGH_MOISTURE_THRESHOLD = 41.0;
    private static final Pattern LACTIC_PATTERN = Pattern.compile("\\blactic\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final Map<String, Integer> milkSourceCounts = new LinkedHashMap<>();
    private final BitSet presentIds = new BitSet();

    private int cheesesAnalyzed;
    private int pasteurizedCount;
    private int rawMilkCount;
    private int organicHighMoistureCount;
    private int validMoistureCount;
    private int lacticFlavourCount;
    private double moistureTotal;

    public CheeseStatistics() {
        milkSourceCounts.put("Cow", 0);
        milkSourceCounts.put("Goat", 0);
        milkSourceCounts.put("Ewe", 0);
        milkSourceCounts.put("Buffalo", 0);
    }

    public void accept(Cheese cheese) {
        Objects.requireNonNull(cheese, "Cheese cannot be null.");

        if (presentIds.get(cheese.id())) {
            throw new IllegalArgumentException("Duplicate cheese ID " + cheese.id() + ".");
        }

        presentIds.set(cheese.id());
        cheesesAnalyzed++;

        countMilkTreatment(cheese.milkTreatmentType());
        countMoisture(cheese);
        countMilkSources(cheese.milkType());

        if (LACTIC_PATTERN.matcher(cheese.flavour()).find()) {
            lacticFlavourCount++;
        }
    }

    public CheeseStatisticsReport createReport() {
        Map.Entry<String, Integer> mostCommonSource = findMostCommonMilkSource();
        double averageMoisture = validMoistureCount == 0 ? 0.0 : moistureTotal / validMoistureCount;

        return new CheeseStatisticsReport(
                cheesesAnalyzed,
                pasteurizedCount,
                rawMilkCount,
                organicHighMoistureCount,
                mostCommonSource.getKey(),
                mostCommonSource.getValue(),
                validMoistureCount,
                averageMoisture,
                lacticFlavourCount);
    }

    // If we want to convert to a Hash approach, this is the code we need to update.
    public BitSet getPresentIds() {
        return (BitSet) presentIds.clone();
    }

    public int getMaximumId() {
        return presentIds.isEmpty() ? 0 : presentIds.length() - 1;
    }

    private void countMilkTreatment(String treatment) {
        if (treatment.equalsIgnoreCase("Pasteurized")) {
            pasteurizedCount++;
        } else if (treatment.equalsIgnoreCase("Raw Milk")) {
            rawMilkCount++;
        }
    }

    private void countMoisture(Cheese cheese) {
        Double moisture = cheese.moisturePercent();

        if (moisture == null) {
            return;
        }

        moistureTotal += moisture;
        validMoistureCount++;

        if (Boolean.TRUE.equals(cheese.organic()) && moisture > HIGH_MOISTURE_THRESHOLD) {
            organicHighMoistureCount++;
        }
    }

    private void countMilkSources(String milkType) {
        if (milkType.isBlank()) {
            return;
        }

        String normalized = milkType.replace(",", " and ");

        for (String source : normalized.split("(?i)\\s+and\\s+")) {
            String canonicalSource = canonicalizeMilkSource(source);

            if (canonicalSource != null) {
                milkSourceCounts.computeIfPresent(canonicalSource, (ignored, count) -> count + 1);
            }
        }
    }

    private String canonicalizeMilkSource(String source) {
        return switch (source.strip().toLowerCase(Locale.ROOT)) {
            case "cow" -> "Cow";
            case "goat" -> "Goat";
            case "ewe" -> "Ewe";
            case "buffalo", "buffalo cow" -> "Buffalo";
            default -> null;
        };
    }

    private Map.Entry<String, Integer> findMostCommonMilkSource() {
        Map.Entry<String, Integer> mostCommon = Map.entry("", 0);

        for (Map.Entry<String, Integer> entry : milkSourceCounts.entrySet()) {
            if (entry.getValue() > mostCommon.getValue()) {
                mostCommon = Map.entry(entry.getKey(), entry.getValue());
            }
        }

        return mostCommon;
    }
}