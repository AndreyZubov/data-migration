package io.datamigration.batch.model;

/** Result of running a single input row through the mapping + validation pipeline. */
public sealed interface ProcessedRow permits ValidRow, InvalidRow {

    long rowNumber();
}
