package edu.csc214.muensterpuns;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Objects;

/**
 * Finds and writes missing cheese IDs using recursive range subdivision.
 *
 * <p>The search begins at ID 1 because the dataset starts partway through the
 * complete ID sequence. The recursion divides the range in half, producing
 * logarithmic stack depth while missing IDs are written directly to the file.</p>
 */
public final class MissingIdWriter {
    // I decided to play with recursion here.  A simple loop would ahve done the trick
    // just fine, but we are talking about recursion in this module so I thought I would
    // use this as a practice.  The missing IDs task seemed like the best fit.  The one 
    // issue is that doing straight recursion would have yielded a really deep stack which
    // it is best to avoid, so two architectural features to flatten it out a bit.  First, 
    // at each recursion level, I divided the data set in half and traversed them, this flattened
    // out the stack depth quite a bit.  I also used a nextClearBit() method to skip large
    // tracts of contiguous IDs.  No sense processing those.  To keep things efficient, like the
    // rest of the program, I streamed the data rather than holding it in a List.
    //
    // I'm pretty sure that a loop would have been more resource efficient and possibly quicker
    // but, again, I'm trying to work with the material in a productive way.
    
    public static final int FIRST_EXPECTED_ID = 1;
    public static final String OUTPUT_FILENAME = "missing_ids.txt";

    public int write(Path outputPath, BitSet presentIds, int maximumId) throws IOException {
        Objects.requireNonNull(outputPath, "Output path cannot be null.");
        Objects.requireNonNull(presentIds, "Present IDs cannot be null.");

        if (maximumId < 0) {
            throw new IllegalArgumentException("Maximum ID cannot be negative.");
        }

        Path parent = outputPath.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            if (maximumId < FIRST_EXPECTED_ID) {
                return 0;
            }

            return writeRange(writer, presentIds, FIRST_EXPECTED_ID, maximumId);
        }
    }

    private int writeRange(BufferedWriter writer, BitSet presentIds, int lowerBound, int upperBound) throws IOException {
        if (lowerBound > upperBound) {
            return 0;
        }

        int firstMissingId = presentIds.nextClearBit(lowerBound);

        if (firstMissingId > upperBound) {
            return 0;
        }

        if (lowerBound == upperBound) {
            writer.write(Integer.toString(lowerBound));
            writer.newLine();
            return 1;
        }

        int midpoint = lowerBound + (upperBound - lowerBound) / 2;

        return writeRange(writer, presentIds, lowerBound, midpoint)
                + writeRange(writer, presentIds, midpoint + 1, upperBound);
    }
}