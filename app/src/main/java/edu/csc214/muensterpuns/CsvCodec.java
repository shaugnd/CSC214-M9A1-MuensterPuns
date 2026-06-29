package edu.csc214.muensterpuns;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses and serializes individual CSV records.
 *
 * <p>The codec supports empty fields, quoted commas, escaped quotation marks,
 * and line-break characters embedded in field values. It processes one record
 * at a time so the surrounding application can stream the input file.</p>
 */
public final class CsvCodec {
    // Looking at the file,it seemed like there was a non-trivial possibility that 
    // I would run into some malformed rows.  This Codec is built around detecting
    // those rows and reporting on them.  This might be overkill, but I've written
    // these types of routines many times before in other languages, so this was a 
    // good exercise for remembering/learing again, how it is done in Java.
    public List<String> parse(String record) {
        Objects.requireNonNull(record, "CSV record cannot be null.");

        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quotedFieldClosed = false;

        for (int index = 0; index < record.length(); index++) {
            char character = record.charAt(index);

            if (inQuotes) {
                if (character == '"') {
                    if (index + 1 < record.length() && record.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        inQuotes = false;
                        quotedFieldClosed = true;
                    }
                } else {
                    field.append(character);
                }

                continue;
            }

            if (character == ',') {
                fields.add(field.toString());
                field.setLength(0);
                quotedFieldClosed = false;
            } else if (character == '"') {
                if (field.length() > 0 || quotedFieldClosed) {
                    throw new IllegalArgumentException("Unexpected quotation mark at character " + (index + 1) + ".");
                }

                inQuotes = true;
            } else {
                if (quotedFieldClosed) {
                    throw new IllegalArgumentException("Unexpected character after closing quotation mark at character " + (index + 1) + ".");
                }

                field.append(character);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quoted field.");
        }

        fields.add(field.toString());
        return List.copyOf(fields);
    }

    public String format(List<String> fields) {
        Objects.requireNonNull(fields, "CSV fields cannot be null.");

        StringBuilder record = new StringBuilder();

        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                record.append(',');
            }

            String field = Objects.requireNonNull(fields.get(index), "CSV field cannot be null.");

            if (requiresQuotes(field)) {
                record.append('"').append(field.replace("\"", "\"\"")).append('"');
            } else {
                record.append(field);
            }
        }

        return record.toString();
    }

    private boolean requiresQuotes(String field) {
        return field.indexOf(',') >= 0
                || field.indexOf('"') >= 0
                || field.indexOf('\r') >= 0
                || field.indexOf('\n') >= 0;
    }
}
