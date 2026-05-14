package io.datamigration.core.format;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Streaming writer of structured records. Implementations must support writing records one at a
 * time without buffering the whole dataset in memory.
 */
public interface RecordWriter extends Closeable {

    /**
     * Append a single record to the output.
     *
     * @param record ordered map of field name → value
     */
    void write(Map<String, Object> record) throws IOException;

    /** Flush any buffered output. Implementations may make this a no-op. */
    void flush() throws IOException;
}
