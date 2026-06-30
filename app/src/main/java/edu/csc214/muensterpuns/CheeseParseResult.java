package edu.csc214.muensterpuns;

import java.util.List;
import java.util.Objects;

/**
 * Contains a usable cheese record and any nonfatal field problems found while
 * parsing its CSV row.
 *
 * <p>Invalid optional analysis values are omitted from the resulting cheese
 * while valid fields remain available for their applicable calculations.</p>
 */
public record CheeseParseResult(
        Cheese cheese,
        List<String> problems) {

    public CheeseParseResult {
        cheese = Objects.requireNonNull(
                cheese,
                "Cheese cannot be null.");

        problems = List.copyOf(
                Objects.requireNonNull(
                        problems,
                        "Problems cannot be null."));

        for (String problem : problems) {
            Objects.requireNonNull(
                    problem,
                    "Problem description cannot be null.");

            if (problem.isBlank()) {
                throw new IllegalArgumentException(
                        "Problem description cannot be blank.");
            }
        }
    }

    public boolean hasProblems() {
        return !problems.isEmpty();
    }
}