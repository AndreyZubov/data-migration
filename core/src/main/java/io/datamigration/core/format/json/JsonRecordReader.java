package io.datamigration.core.format.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.datamigration.core.format.RecordReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Streaming reader for a JSON array of objects, e.g. {@code [{"a":1},{"a":2}]}.
 *
 * <p>Each element of the top-level array must be a JSON object; objects are parsed lazily, one at a
 * time, to keep memory consumption flat regardless of input size.
 */
public final class JsonRecordReader implements RecordReader {

    private final JsonParser parser;
    private final ObjectMapper mapper;
    private Map<String, Object> next;
    private boolean finished;

    public JsonRecordReader(InputStream in) throws IOException {
        this.mapper = new ObjectMapper();
        this.parser = new JsonFactory().createParser(in);
        JsonToken first = parser.nextToken();
        if (first != JsonToken.START_ARRAY) {
            throw new IOException("Expected top-level JSON array, got " + first);
        }
        advance();
    }

    private void advance() throws IOException {
        if (finished) {
            return;
        }
        JsonToken t = parser.nextToken();
        if (t == null || t == JsonToken.END_ARRAY) {
            next = null;
            finished = true;
            return;
        }
        if (t != JsonToken.START_OBJECT) {
            throw new IOException("Expected JSON object in array, got " + t);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> row = mapper.readValue(parser, LinkedHashMap.class);
        next = row;
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public Map<String, Object> next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Map<String, Object> current = next;
        try {
            advance();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return current;
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }
}
