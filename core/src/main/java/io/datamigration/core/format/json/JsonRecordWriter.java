package io.datamigration.core.format.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.datamigration.core.format.RecordWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Streaming writer that emits records as a JSON array of objects.
 *
 * <p>The opening {@code [} is written on construction; the closing {@code ]} is written on {@link
 * #close()}.
 */
public final class JsonRecordWriter implements RecordWriter {

    private final JsonGenerator generator;
    private final ObjectMapper mapper;
    private boolean closed;

    public JsonRecordWriter(OutputStream out) throws IOException {
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.generator = new JsonFactory().createGenerator(out, JsonEncoding.UTF8);
        this.generator.useDefaultPrettyPrinter();
        this.generator.writeStartArray();
    }

    @Override
    public void write(Map<String, Object> record) throws IOException {
        mapper.writeValue(generator, record);
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        generator.writeEndArray();
        generator.close();
    }
}
