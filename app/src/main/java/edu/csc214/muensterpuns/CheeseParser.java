package edu.csc214.muensterpuns;

import java.util.List;
import java.util.Objects;

/**
 * Converts schema-aligned CSV fields into an immutable {@link Cheese}.
 *
 * <p>Missing optional values are preserved as absent values. Invalid IDs,
 * percentages, organic flags, or record widths produce a field-specific
 * {@link MalformedCheeseException}.</p>
 */
public final class CheeseParser {
    // This class is the last step in abstracting the business of getting the data
    // out of the file and into a format that we can reasonably use.  here we are 
    // MAKING CHEESE!
    
    public Cheese parse(List<String> fields, CheeseSchema schema) {
        Objects.requireNonNull(fields, "CSV fields cannot be null.");
        Objects.requireNonNull(schema, "Cheese schema cannot be null.");

        String recordId = recoverRecordId(fields, schema);

        try {
            schema.validateFieldCount(fields);
        } catch (IllegalArgumentException exception) {
            throw new MalformedCheeseException(recordId, exception.getMessage());
        }

        int id = parseId(
                    getField(fields, schema.cheeseIdIndex(),    
                    "CheeseId", recordId), recordId);
        Double moisturePercent = parseMoisture(
                    getField(fields, schema.moisturePercentIndex(), 
                    "MoisturePercent", recordId), recordId);
        String flavour = getField(fields,
                                    schema.flavourIndex(), 
                                    "FlavourEn", recordId);
        Boolean organic = parseOrganic(
                            getField(fields, 
                                schema.organicIndex(), 
                                "Organic", recordId), recordId);
        String milkType = getField(
                            fields, schema.milkTypeIndex(), 
                            "MilkTypeEn", recordId);
        String milkTreatmentType = getField(
                                    fields, schema.milkTreatmentTypeIndex(), 
                                    "MilkTreatmentTypeEn", recordId);

        try {
            return new Cheese(id, moisturePercent, flavour, organic, milkType, milkTreatmentType);
        } catch (IllegalArgumentException exception) {
            throw new MalformedCheeseException(recordId, exception.getMessage());
        }
    }

    private int parseId(String value, String recordId) {
        String normalized = value.strip();

        if (normalized.isEmpty()) {
            throw new MalformedCheeseException(recordId, "Cheese ID is blank.");
        }

        try {
            int id = Integer.parseInt(normalized);

            if (id <= 0) {
                throw new MalformedCheeseException(recordId, "Cheese ID must be positive.");
            }

            return id;
        } catch (NumberFormatException exception) {
            throw new MalformedCheeseException(recordId, "Invalid cheese ID \"" + normalized + "\".");
        }
    }

    private Double parseMoisture(String value, String recordId) {
        String normalized = value.strip();

        if (normalized.isEmpty()) {
            return null;
        }

        try {
            double moisture = Double.parseDouble(normalized);

            if (!Double.isFinite(moisture) || moisture < 0.0 || moisture > 100.0) {
                throw new MalformedCheeseException(recordId, "Moisture percentage must be between 0 and 100.");
            }

            return moisture;
        } catch (NumberFormatException exception) {
            throw new MalformedCheeseException(recordId, "Invalid moisture value \"" + normalized + "\".");
        }
    }

    private Boolean parseOrganic(String value, String recordId) {
        String normalized = value.strip();

        if (normalized.isEmpty()) {
            return null;
        }

        return switch (normalized) {
            case "1" -> true;
            case "0" -> false;
            default -> throw new MalformedCheeseException(recordId, "Invalid organic value \"" + normalized + "\". Expected 0 or 1.");
        };
    }

    private String getField(List<String> fields, int index, String fieldName, String recordId) {
        String value = fields.get(index);

        if (value == null) {
            throw new MalformedCheeseException(recordId, fieldName + " cannot be null.");
        }

        return value;
    }

    private String recoverRecordId(List<String> fields, CheeseSchema schema) {
        int index = schema.cheeseIdIndex();

        if (index >= fields.size()) {
            return null;
        }

        String value = fields.get(index);
        return value == null ? null : value;
    }
}