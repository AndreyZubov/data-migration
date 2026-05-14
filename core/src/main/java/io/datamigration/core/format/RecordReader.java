package io.datamigration.core.format;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Streaming reader of structured records. Implementations should be lazy and avoid loading the
 * whole input into memory, allowing safe consumption of multi-million-row files.
 */
public interface RecordReader extends Iterator<Map<String, Object>>, Closeable {

    /**
     * Adapt this reader to a {@link Stream}. The returned stream is sequential and ordered; closing
     * it closes this reader.
     */
    default Stream<Map<String, Object>> stream() {
        Spliterator<Map<String, Object>> spliterator =
                Spliterators.spliteratorUnknownSize(
                        this, Spliterator.ORDERED | Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false)
                .onClose(() -> {
                    try {
                        close();
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
    }
}
