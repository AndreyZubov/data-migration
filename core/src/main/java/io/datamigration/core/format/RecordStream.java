package io.datamigration.core.format;

import io.datamigration.core.format.csv.CsvRecordReader;
import io.datamigration.core.format.csv.CsvRecordWriter;
import io.datamigration.core.format.json.JsonRecordReader;
import io.datamigration.core.format.json.JsonRecordWriter;
import io.datamigration.core.format.xlsx.XlsxRecordReader;
import io.datamigration.core.format.xlsx.XlsxRecordWriter;
import io.datamigration.core.format.xml.XmlRecordReader;
import io.datamigration.core.format.xml.XmlRecordWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Factory methods for opening {@link RecordReader}s and {@link RecordWriter}s by format. */
public final class RecordStream {

    /** Supported record stream formats. */
    public enum Format {
        JSON,
        XML,
        CSV,
        XLSX;

        /** Resolve a {@link Format} by file extension (case-insensitive, without leading dot). */
        public static Format fromExtension(String extension) {
            Objects.requireNonNull(extension, "extension");
            String normalized = extension.toLowerCase(Locale.ROOT);
            if (normalized.startsWith(".")) {
                normalized = normalized.substring(1);
            }
            return switch (normalized) {
                case "json" -> JSON;
                case "xml" -> XML;
                case "csv" -> CSV;
                case "xlsx" -> XLSX;
                default -> throw new IllegalArgumentException("Unsupported extension: " + extension);
            };
        }
    }

    private RecordStream() {}

    public static RecordReader reader(Format format, InputStream in) throws IOException {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(in, "in");
        return switch (format) {
            case JSON -> new JsonRecordReader(in);
            case XML -> new XmlRecordReader(in);
            case CSV -> new CsvRecordReader(in);
            case XLSX -> new XlsxRecordReader(in);
        };
    }

    public static RecordWriter writer(Format format, OutputStream out, List<String> headers)
            throws IOException {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(headers, "headers");
        return switch (format) {
            case JSON -> new JsonRecordWriter(out);
            case XML -> new XmlRecordWriter(out);
            case CSV -> new CsvRecordWriter(out, headers);
            case XLSX -> new XlsxRecordWriter(out, headers);
        };
    }
}
