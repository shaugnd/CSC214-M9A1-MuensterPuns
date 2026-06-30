package edu.csc214.muensterpuns;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Streams transformed copies of the cheese CSV dataset.
 *
 * <p>One output omits the original header row. The other preserves the header
 * and data rows while removing the CheeseId column. Records are written as they
 * are processed so the complete dataset does not need to remain in memory.</p>
 */
public final class CheeseCsvOutputWriter implements AutoCloseable {
    // Here is am esssentially splitting the output stream and filtering for the proper values
    // to go into each output file.  I did not parameterize this because the assignment did not
    // ask for it, but if I was doing this for real, I would have made these output files
    // definable on the command line with flags and parameters.
    //
    // This class creates the output directory if necessary and then writes the reduced header
    //  to the no-ID file.  It uses CsvCodec to ensure proper CSV quoting is preserved and overwrites
    // old output files on each run.  The class writes only one record at a time.
    
    public static final String WITHOUT_HEADERS_FILENAME = "cheese_without_headers.csv";
    public static final String WITHOUT_IDS_FILENAME = "cheese_without_ids.csv";

    private final CsvCodec csvCodec;
    private final CheeseSchema schema;
    private final Path withoutHeadersPath;
    private final Path withoutIdsPath;
    private final BufferedWriter withoutHeadersWriter;
    private final BufferedWriter withoutIdsWriter;

    private boolean closed;

    public CheeseCsvOutputWriter(
            Path outputDirectory,
            CsvCodec csvCodec,
            CheeseSchema schema,
            List<String> headerFields) throws IOException {

        Objects.requireNonNull(outputDirectory, "Output directory cannot be null.");
        this.csvCodec = Objects.requireNonNull(csvCodec, "CSV codec cannot be null.");
        this.schema = Objects.requireNonNull(schema, "Cheese schema cannot be null.");
        Objects.requireNonNull(headerFields, "Header fields cannot be null.");

        schema.validateFieldCount(headerFields);
        Files.createDirectories(outputDirectory);

        withoutHeadersPath = outputDirectory.resolve(WITHOUT_HEADERS_FILENAME);
        withoutIdsPath = outputDirectory.resolve(WITHOUT_IDS_FILENAME);

        withoutHeadersWriter = openWriter(withoutHeadersPath);

        BufferedWriter idWriter = null;

        try {
            idWriter = openWriter(withoutIdsPath);
            writeLine(idWriter, csvCodec.format(removeIdField(headerFields)));
        } catch (IOException | RuntimeException exception) {
            closeAfterConstructionFailure(withoutHeadersWriter, idWriter, exception);
            throw exception;
        }

        withoutIdsWriter = idWriter;
    }

    public void writeRecord(List<String> fields) throws IOException {
        ensureOpen();
        Objects.requireNonNull(fields, "CSV fields cannot be null.");
        schema.validateFieldCount(fields);

        String completeRecord = csvCodec.format(fields);
        String recordWithoutId = csvCodec.format(removeIdField(fields));

        writeLine(withoutHeadersWriter, completeRecord);
        writeLine(withoutIdsWriter, recordWithoutId);
    }

    public Path withoutHeadersPath() {
        return withoutHeadersPath;
    }

    public Path withoutIdsPath() {
        return withoutIdsPath;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;
        IOException failure = null;

        try {
            withoutHeadersWriter.close();
        } catch (IOException exception) {
            failure = exception;
        }

        try {
            withoutIdsWriter.close();
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private BufferedWriter openWriter(Path path) throws IOException {
        return Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private List<String> removeIdField(List<String> fields) {
        List<String> transformedFields = new ArrayList<>(fields);
        transformedFields.remove(schema.cheeseIdIndex());
        return transformedFields;
    }

    private void writeLine(BufferedWriter writer, String record) throws IOException {
        writer.write(record);
        writer.newLine();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("CSV output writer is closed.");
        }
    }

    private void closeAfterConstructionFailure(
            BufferedWriter firstWriter,
            BufferedWriter secondWriter,
            Exception originalFailure) {

        try {
            firstWriter.close();
        } catch (IOException closeFailure) {
            originalFailure.addSuppressed(closeFailure);
        }

        if (secondWriter != null) {
            try {
                secondWriter.close();
            } catch (IOException closeFailure) {
                originalFailure.addSuppressed(closeFailure);
            }
        }
    }
}
