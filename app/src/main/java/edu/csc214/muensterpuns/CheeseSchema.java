package edu.csc214.muensterpuns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes the location of required fields within the cheese CSV file.
 *
 * <p>The schema is created from the file's header row so downstream code can
 * access columns by meaning rather than relying on hard-coded positions.</p>
 */
public record CheeseSchema(
    // I've mentioned before that I have issues with magic numbers.  The idea
    // here is to avoid those entirely by intentionally building a schema for 
    // every file we read.  This way, if the columns get moved around for some
    // reason, we don't ahve to rewrite any code . . . hopefully.
        int columnCount,
        int cheeseIdIndex,
        int moisturePercentIndex,
        int flavourIndex,
        int organicIndex,
        int milkTypeIndex,
        int milkTreatmentTypeIndex) {

    private static final String CHEESE_ID = "CheeseId";
    private static final String MOISTURE_PERCENT = "MoisturePercent";
    private static final String FLAVOUR = "FlavourEn";
    private static final String ORGANIC = "Organic";
    private static final String MILK_TYPE = "MilkTypeEn";
    private static final String MILK_TREATMENT_TYPE = "MilkTreatmentTypeEn";

    public CheeseSchema {
        if (columnCount <= 0) {
            throw new IllegalArgumentException("Column count must be positive.");
        }

        validateIndex(cheeseIdIndex, columnCount, CHEESE_ID);
        validateIndex(moisturePercentIndex, columnCount, MOISTURE_PERCENT);
        validateIndex(flavourIndex, columnCount, FLAVOUR);
        validateIndex(organicIndex, columnCount, ORGANIC);
        validateIndex(milkTypeIndex, columnCount, MILK_TYPE);
        validateIndex(milkTreatmentTypeIndex, columnCount, MILK_TREATMENT_TYPE);
    }

    public static CheeseSchema fromHeader(List<String> headerFields) {
        Objects.requireNonNull(headerFields, "Header fields cannot be null.");

        if (headerFields.isEmpty()) {
            throw new IllegalArgumentException("CSV header cannot be empty.");
        }

        Map<String, Integer> indexes = new HashMap<>();

        for (int index = 0; index < headerFields.size(); index++) {
            String header = Objects.requireNonNull(headerFields.get(index), "Header field cannot be null.").strip();

            if (index == 0 && header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }

            if (header.isEmpty()) {
                throw new IllegalArgumentException("Header field at column " + (index + 1) + " is blank.");
            }

            Integer previousIndex = indexes.putIfAbsent(header, index);

            if (previousIndex != null) {
                throw new IllegalArgumentException("Duplicate CSV header: " + header + ".");
            }
        }

        return new CheeseSchema(
                headerFields.size(),
                requireIndex(indexes, CHEESE_ID),
                requireIndex(indexes, MOISTURE_PERCENT),
                requireIndex(indexes, FLAVOUR),
                requireIndex(indexes, ORGANIC),
                requireIndex(indexes, MILK_TYPE),
                requireIndex(indexes, MILK_TREATMENT_TYPE));
    }

    public void validateFieldCount(List<String> fields) {
        Objects.requireNonNull(fields, "CSV fields cannot be null.");

        if (fields.size() != columnCount) {
            throw new IllegalArgumentException("Expected " + columnCount + " columns but found " + fields.size() + ".");
        }
    }

    private static int requireIndex(Map<String, Integer> indexes, String headerName) {
        Integer index = indexes.get(headerName);

        if (index == null) {
            throw new IllegalArgumentException("Required CSV header is missing: " + headerName + ".");
        }

        return index;
    }

    private static void validateIndex(int index, int columnCount, String headerName) {
        if (index < 0 || index >= columnCount) {
            throw new IllegalArgumentException("Index for " + headerName + " is outside the schema.");
        }
    }
}
