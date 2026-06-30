package edu.csc214.muensterpuns;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Streams malformed-record diagnostics to a console-compatible writer.
 *
 * <p>The production constructor writes to standard error so diagnostics remain
 * separate from the normal program summary written to standard output.</p>
 */
public final class ConsoleMalformedRowReporter implements MalformedRowReporter {
    // This is not strictly necessary from the assignment perspective, but I 
    // personally do NOT like command line utilities that offer no feedback
    // on how the process went.  Success?  Failure?  Exceptions?  Something?
    // It is fine for things like rm or cp or cd where the results are easy to 
    // check or obvious, but for something like this I need more summary information.
    // Since we have all of the info it is just a matter of duplicating the output
    // to the console and if we are going to do that, we might as well stream error 
    // records to the console as they happen as well.
    
    private final PrintWriter writer;

    public ConsoleMalformedRowReporter() {
        this(new OutputStreamWriter(System.err, StandardCharsets.UTF_8));
    }

    ConsoleMalformedRowReporter(Writer writer) {
        this.writer = new PrintWriter(
                Objects.requireNonNull(
                        writer,
                        "Writer cannot be null."),
                true);
    }

    @Override
    public void report(
            long fileRowNumber,
            String recordId,
            String problem) {

        if (fileRowNumber <= 0) {
            throw new IllegalArgumentException(
                    "File row number must be positive.");
        }

        String normalizedId =
                recordId == null || recordId.isBlank()
                        ? "unavailable"
                        : recordId.strip();

        String normalizedProblem = Objects.requireNonNull(
                problem,
                "Problem description cannot be null.").strip();

        if (normalizedProblem.isEmpty()) {
            throw new IllegalArgumentException(
                    "Problem description cannot be blank.");
        }

        writer.println(
                "Row "
                        + fileRowNumber
                        + ", CheeseId "
                        + normalizedId
                        + ": "
                        + normalizedProblem);
    }
}