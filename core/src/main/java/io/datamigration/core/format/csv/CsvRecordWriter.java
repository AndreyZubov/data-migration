package io.datamigration.core.format.csv;

import com.opencsv.CSVWriter;
import io.datamigration.core.format.RecordWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Streaming CSV writer (UTF-8). Emits the header row on first {@link #write(Map)} call. */
public final class CsvRecordWriter implements RecordWriter {

    private final CSVWriter csv;
    private final Writer sink;
    private final List<String> headers;
    private boolean headerEmitted;

    public CsvRecordWriter(OutputStream out, List<String> headers) {
        this.sink = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        this.csv = new CSVWriter(sink);
        this.headers = List.copyOf(headers);
    }

    @Override
    public void write(Map<String, Object> record) {
        if (!headerEmitted) {
            csv.writeNext(headers.toArray(new String[0]));
            headerEmitted = true;
        }
        String[] row = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            Object value = record.get(headers.get(i));
            row[i] = value == null ? "" : value.toString();
        }
        csv.writeNext(row);
    }

    @Override
    public void flush() throws IOException {
        csv.flush();
    }

    @Override
    public void close() throws IOException {
        if (!headerEmitted) {
            csv.writeNext(headers.toArray(new String[0]));
        }
        csv.close();
    }
}
