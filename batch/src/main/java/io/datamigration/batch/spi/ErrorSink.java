package io.datamigration.batch.spi;

import io.datamigration.batch.model.InvalidRow;
import java.util.List;

/**
 * Persists rows rejected by validation. Implemented by the outer module that owns persistence; the
 * batch module only consumes this abstraction so it does not need to know how errors are stored.
 */
public interface ErrorSink {

    void persist(String jobId, List<InvalidRow> invalidRows);
}
