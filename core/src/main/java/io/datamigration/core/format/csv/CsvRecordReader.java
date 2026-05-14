package io.datamigration.core.format.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.datamigration.core.format.RecordReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Streaming CSV reader (UTF-8). The first line is treated as the header row; subsequent rows are
 * yielded as ordered maps keyed by header name.
 */
public final class CsvRecordReader implements RecordReader {

    private final CSVReader csv;
    private final Reader sourceReader;
    private final String[] headers;
    private Map<String, Object> next;

    public CsvRecordReader(InputStream in) throws IOException {
        this.sourceReader = new InputStreamReader(in, StandardCharsets.UTF_8);
        this.csv = new CSVReader(sourceReader);
        try {
            String[] header = csv.readNext();
            this.headers = header == null ? new String[0] : header;
        } catch (CsvValidationException e) {
            throw new IOException(e);
        }
        advance();
    }

    private void advance() {
        try {
            String[] row = csv.readNext();
            if (row == null) {
                next = null;
                return;
            }
            Map<String, Object> map = new LinkedHashMap<>(headers.length);
            for (int i = 0; i < headers.length; i++) {
                map.put(headers[i], i < row.length ? row[i] : "");
            }
            next = map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (CsvValidationException e) {
            throw new UncheckedIOException(new IOException(e));
        }
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
        advance();
        return current;
    }

    @Override
    public void close() throws IOException {
        csv.close();
        sourceReader.close();
    }
}
