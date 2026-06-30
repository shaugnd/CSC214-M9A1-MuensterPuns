package edu.csc214.muensterpuns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;

/**
 * Formats and writes the completed cheese-analysis report.
 *
 * <p>This class performs no statistical calculations. It receives an immutable
 * report and serializes it as human-readable text.</p>
 */
public final class CheeseReportWriter {
    // I've decoupled this from the actual output channel so that I can use
    // it to write both the file output AND a console report to give a visual
    // indicator of successful execution and the results of that execution.

    public String format(CheeseStatisticsReport report) {
        Objects.requireNonNull(report, "Statistics report cannot be null.");

        String milkSource = report.mostCommonMilkSource().isEmpty()
                ? "Not available"
                : report.mostCommonMilkSource()
                        + " (" + report.mostCommonMilkSourceCount() + " cheeses)";

        return String.format(
                Locale.ROOT,
                """
                Canadian Cheese Analysis
                Cheeses analyzed: %d
                Pasteurized cheeses: %d
                Raw-milk cheeses: %d
                Organic cheeses above 41.0%% moisture: %d
                Most common milk source: %s
                Valid moisture values: %d
                Average moisture: %.2f%%
                Flavour descriptions containing "lactic": %d
                """,
                report.cheesesAnalyzed(),
                report.pasteurizedCount(),
                report.rawMilkCount(),
                report.organicHighMoistureCount(),
                milkSource,
                report.validMoistureCount(),
                report.averageMoisturePercent(),
                report.lacticFlavourCount());
    }

    public void write(Path outputPath, CheeseStatisticsReport report)
            throws IOException {

        Objects.requireNonNull(outputPath, "Output path cannot be null.");
        Objects.requireNonNull(report, "Statistics report cannot be null.");

        Path parent = outputPath.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                outputPath,
                format(report),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
}