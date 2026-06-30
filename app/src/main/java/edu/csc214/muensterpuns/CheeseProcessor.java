
    // This is the main data stream coordinator. It reads the CSV one row at a time, 
    // parses the header and builds the schema, then writes the two transformed CSV
    // files in real time as rows are processed.  Along the way, it converts valid rows into 
    // Cheese records annd updates stats without retaining the dtaset.  This class 
    // will report malformed rows immediately.  It writes output.txt and missing_ids.txt
    // and returns a CheeseProcessingResult.  A row with a valid CSV structure but one
    // or more bad analysis values, such as invalid moisture value, is still copied into 
    // the transformed CSV file.  It is, however excluded from the stats reporting and
    // reported as 'malformed'

package edu.csc214.muensterpuns;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates one streaming pass through the cheese CSV dataset.
 *
 * <p>Each physical data row is parsed, written to the transformed CSV outputs,
 * converted into a {@link Cheese}, and analyzed before the next row is read.
 * Structurally valid rows remain in the transformed CSV files even when one of
 * their analysis fields is malformed.</p>
 */
public final class CheeseProcessor {
    public static final String REPORT_FILENAME = "output.txt";

    private final CsvCodec csvCodec;
    private final CheeseParser cheeseParser;
    private final CheeseReportWriter reportWriter;
    private final MissingIdWriter missingIdWriter;
    private final MalformedRowReporter malformedRowReporter;

    public CheeseProcessor() {
        this(
                new CsvCodec(),
                new CheeseParser(),
                new CheeseReportWriter(),
                new MissingIdWriter(),
                new ConsoleMalformedRowReporter());
    }

    CheeseProcessor(
            CsvCodec csvCodec,
            CheeseParser cheeseParser,
            CheeseReportWriter reportWriter,
            MissingIdWriter missingIdWriter,
            MalformedRowReporter malformedRowReporter) {

        this.csvCodec = Objects.requireNonNull(
                csvCodec,
                "CSV codec cannot be null.");

        this.cheeseParser = Objects.requireNonNull(
                cheeseParser,
                "Cheese parser cannot be null.");

        this.reportWriter = Objects.requireNonNull(
                reportWriter,
                "Report writer cannot be null.");

        this.missingIdWriter = Objects.requireNonNull(
                missingIdWriter,
                "Missing-ID writer cannot be null.");

        this.malformedRowReporter = Objects.requireNonNull(
                malformedRowReporter,
                "Malformed-row reporter cannot be null.");
    }

    public CheeseProcessingResult process(
            Path inputPath,
            Path outputDirectory) throws IOException {

        Objects.requireNonNull(
                inputPath,
                "Input path cannot be null.");

        Objects.requireNonNull(
                outputDirectory,
                "Output directory cannot be null.");

        CheeseStatistics statistics = new CheeseStatistics();
        int malformedRowCount = 0;

        Path withoutHeadersPath = outputDirectory.resolve(
                CheeseCsvOutputWriter.WITHOUT_HEADERS_FILENAME);

        Path withoutIdsPath = outputDirectory.resolve(
                CheeseCsvOutputWriter.WITHOUT_IDS_FILENAME);

        try (BufferedReader reader = Files.newBufferedReader(
                inputPath,
                StandardCharsets.UTF_8)) {

            String headerRecord = reader.readLine();

            if (headerRecord == null) {
                throw new IllegalArgumentException(
                        "Input CSV file is empty.");
            }

            List<String> headerFields = parseHeader(headerRecord);
            CheeseSchema schema = CheeseSchema.fromHeader(headerFields);

            try (CheeseCsvOutputWriter csvOutputWriter =
                    new CheeseCsvOutputWriter(
                            outputDirectory,
                            csvCodec,
                            schema,
                            headerFields)) {

                String record;
                long fileRowNumber = 1;

                while ((record = reader.readLine()) != null) {
                    fileRowNumber++;

                    List<String> fields;

                    try {
                        fields = csvCodec.parse(record);
                    } catch (IllegalArgumentException exception) {
                        malformedRowReporter.report(
                                fileRowNumber,
                                recoverIdFromRawRecord(record, schema),
                                exception.getMessage());

                        malformedRowCount++;
                        continue;
                    }

                    try {
                        schema.validateFieldCount(fields);
                    } catch (IllegalArgumentException exception) {
                        malformedRowReporter.report(
                                fileRowNumber,
                                recoverIdFromFields(fields, schema),
                                exception.getMessage());

                        malformedRowCount++;
                        continue;
                    }

                    csvOutputWriter.writeRecord(fields);

                    try {
                        CheeseParseResult parseResult = cheeseParser.parse(fields, schema);
                        boolean rowHasProblem = false;

                        for (String problem : parseResult.problems()) {
                                malformedRowReporter.report(
                                        fileRowNumber,
                                        Integer.toString(parseResult.cheese().id()),
                                        problem);

                                rowHasProblem = true;
                        }

                        try {
                                statistics.accept(parseResult.cheese());
                        } catch (IllegalArgumentException exception) {
                                malformedRowReporter.report(
                                        fileRowNumber,
                                        Integer.toString(parseResult.cheese().id()),
                                        exception.getMessage());

                                rowHasProblem = true;
                        }

                        if (rowHasProblem) {
                                malformedRowCount++;
                        }
                        } catch (MalformedCheeseException exception) {
                        malformedRowReporter.report(
                                fileRowNumber,
                                exception.recordId(),
                                exception.getMessage());

                        malformedRowCount++;
                        }
                        
                }
            }
        }

        CheeseStatisticsReport statisticsReport =
                statistics.createReport();

        Path reportPath = outputDirectory.resolve(
                REPORT_FILENAME);

        reportWriter.write(
                reportPath,
                statisticsReport);

        Path missingIdsPath = outputDirectory.resolve(
                MissingIdWriter.OUTPUT_FILENAME);

        int missingIdCount = missingIdWriter.write(
                missingIdsPath,
                statistics.getPresentIds(),
                statistics.getMaximumId());

        return new CheeseProcessingResult(
                statisticsReport,
                malformedRowCount,
                missingIdCount,
                reportPath,
                withoutHeadersPath,
                withoutIdsPath,
                missingIdsPath);
    }

    private List<String> parseHeader(String headerRecord) {
        try {
            return csvCodec.parse(headerRecord);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid CSV header: "
                            + exception.getMessage(),
                    exception);
        }
    }

    private String recoverIdFromFields(
            List<String> fields,
            CheeseSchema schema) {

        int idIndex = schema.cheeseIdIndex();

        if (idIndex >= fields.size()) {
            return null;
        }

        String value = fields.get(idIndex);

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }

    private String recoverIdFromRawRecord(
            String record,
            CheeseSchema schema) {

        if (schema.cheeseIdIndex() != 0) {
            return null;
        }

        int commaIndex = record.indexOf(',');
        String candidate = commaIndex < 0
                ? record
                : record.substring(0, commaIndex);

        candidate = candidate.strip();

        if (candidate.length() >= 2
                && candidate.startsWith("\"")
                && candidate.endsWith("\"")) {

            candidate = candidate
                    .substring(1, candidate.length() - 1)
                    .replace("\"\"", "\"")
                    .strip();
        }

        return candidate.isEmpty() ? null : candidate;
    }
}
