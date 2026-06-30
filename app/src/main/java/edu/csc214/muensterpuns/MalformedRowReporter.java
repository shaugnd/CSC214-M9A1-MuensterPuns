package edu.csc214.muensterpuns;

/**
 * Reports a malformed CSV record as soon as it is encountered.
 *
 * <p>Each diagnostic includes the original physical file row number, any
 * recoverable record ID, and a description of the problem.</p>
 */
@FunctionalInterface
public interface MalformedRowReporter {
    void report(long fileRowNumber, String recordId, String problem);
}