package edu.csc214.muensterpuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests recursive missing-ID detection and direct UTF-8 output in
 * {@link MissingIdWriter}.
 *
 * <p>The tests verify that the search begins at ID 1, preserves ascending
 * order, handles complete and empty ranges, creates directories, overwrites
 * existing files, and validates its inputs.</p>
 */
class MissingIdWriterTest {
    private final MissingIdWriter writer = new MissingIdWriter();

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesMissingPrefixBeforeFirstObservedId() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(3);
        presentIds.set(4);
        presentIds.set(5);

        Path output = temporaryDirectory.resolve("missing_ids.txt");

        int missingCount = writer.write(output, presentIds, 5);

        assertEquals(2, missingCount);
        assertEquals(List.of("1", "2"), readLines(output));
    }

    @Test
    void writesInternalAndTrailingGapsInAscendingOrder() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(2);
        presentIds.set(3);
        presentIds.set(5);
        presentIds.set(8);

        Path output = temporaryDirectory.resolve("missing_ids.txt");

        int missingCount = writer.write(output, presentIds, 8);

        assertEquals(4, missingCount);
        assertEquals(List.of("1", "4", "6", "7"), readLines(output));
    }

    @Test
    void writesEveryIdWhenNoIdsArePresent() throws IOException {
        BitSet presentIds = new BitSet();
        Path output = temporaryDirectory.resolve("missing_ids.txt");

        int missingCount = writer.write(output, presentIds, 5);

        assertEquals(5, missingCount);
        assertEquals(List.of("1", "2", "3", "4", "5"), readLines(output));
    }

    @Test
    void writesEmptyFileWhenEveryIdIsPresent() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(1, 6);
        Path output = temporaryDirectory.resolve("missing_ids.txt");

        int missingCount = writer.write(output, presentIds, 5);

        assertEquals(0, missingCount);
        assertTrue(Files.exists(output));
        assertEquals(List.of(), readLines(output));
    }

    @Test
    void maximumIdZeroProducesEmptyFile() throws IOException {
        BitSet presentIds = new BitSet();
        Path output = temporaryDirectory.resolve("missing_ids.txt");

        int missingCount = writer.write(output, presentIds, 0);

        assertEquals(0, missingCount);
        assertEquals(List.of(), readLines(output));
    }

    @Test
    void ignoresPresentIdsAboveMaximumId() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(1);
        presentIds.set(2);
        presentIds.set(100);
        Path output = temporaryDirectory.resolve("missing_ids.txt");

        int missingCount = writer.write(output, presentIds, 3);

        assertEquals(1, missingCount);
        assertEquals(List.of("3"), readLines(output));
    }

    @Test
    void createsMissingParentDirectories() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(1);
        Path output = temporaryDirectory
                .resolve("generated")
                .resolve("reports")
                .resolve("missing_ids.txt");

        writer.write(output, presentIds, 2);

        assertTrue(Files.exists(output));
        assertEquals(List.of("2"), readLines(output));
    }

    @Test
    void overwritesExistingFile() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(1);
        presentIds.set(3);

        Path output = temporaryDirectory.resolve("missing_ids.txt");
        Files.writeString(
                output,
                "obsolete content",
                StandardCharsets.UTF_8);

        writer.write(output, presentIds, 3);

        assertEquals(List.of("2"), readLines(output));
    }

    @Test
    void handlesLargeRangeWithShallowRecursiveDepth() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(1, 2392);
        presentIds.clear(1);
        presentIds.clear(228);
        presentIds.clear(1200);
        presentIds.clear(2391);

        Path output = temporaryDirectory.resolve("missing_ids.txt");

        int missingCount = writer.write(output, presentIds, 2391);

        assertEquals(4, missingCount);
        assertEquals(List.of("1", "228", "1200", "2391"), readLines(output));
    }

    @Test
    void doesNotModifySuppliedBitSet() throws IOException {
        BitSet presentIds = new BitSet();
        presentIds.set(2);
        BitSet original = (BitSet) presentIds.clone();

        writer.write(
                temporaryDirectory.resolve("missing_ids.txt"),
                presentIds,
                3);

        assertEquals(original, presentIds);
    }

    @Test
    void rejectsNegativeMaximumId() {
        BitSet presentIds = new BitSet();

        assertThrows(
                IllegalArgumentException.class,
                () -> writer.write(
                        temporaryDirectory.resolve("missing_ids.txt"),
                        presentIds,
                        -1));
    }

    @Test
    void rejectsNullOutputPath() {
        BitSet presentIds = new BitSet();

        assertThrows(
                NullPointerException.class,
                () -> writer.write(null, presentIds, 5));
    }

    @Test
    void rejectsNullPresentIds() {
        Path output = temporaryDirectory.resolve("missing_ids.txt");

        assertThrows(
                NullPointerException.class,
                () -> writer.write(output, null, 5));
    }

    private static List<String> readLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }
}
