package edu.csc214.muensterpuns;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Starts the Canadian cheese data-processing application.
 *
 * <p>The application uses {@code data/cheese_data.csv} by default. A different
 * input filename may be supplied as the single command-line argument.</p>
 */
public final class App {
    private static final Path DEFAULT_INPUT_PATH =
            Path.of("data", "cheese_data.csv");

    private static final Path OUTPUT_DIRECTORY =
            Path.of(".");

    private App() {
    }

    public static void main(String[] args) {
        try {
            Path inputPath = resolveInputPath(args);

            System.out.println(
                    "Processing "
                            + inputPath.toAbsolutePath().normalize()
                            + "...");

            CheeseProcessingResult result =
                    new CheeseProcessor().process(
                            inputPath,
                            OUTPUT_DIRECTORY);

            System.out.println();
            System.out.print(
                    new CheeseReportWriter().format(
                            result.statisticsReport()));

            System.out.println();
            System.out.println("Processing complete.");
            System.out.println(
                    "Malformed rows: "
                            + result.malformedRowCount());

            System.out.println(
                    "Missing IDs written: "
                            + result.missingIdCount());

            System.out.println(
                    "Output location: "
                            + OUTPUT_DIRECTORY
                                    .toAbsolutePath()
                                    .normalize());
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println(
                    "Processing failed: "
                            + exception.getMessage());
        }
    }

    static Path resolveInputPath(String[] args) {
        Objects.requireNonNull(
                args,
                "Arguments cannot be null.");

        if (args.length > 1) {
            throw new IllegalArgumentException(
                    "Usage: gradlew run --args=\"[input-file]\"");
        }

        if (args.length == 0) {
            return DEFAULT_INPUT_PATH;
        }

        if (args[0] == null || args[0].isBlank()) {
            throw new IllegalArgumentException(
                    "Input filename cannot be blank.");
        }

        return Path.of(args[0]);
    }
}