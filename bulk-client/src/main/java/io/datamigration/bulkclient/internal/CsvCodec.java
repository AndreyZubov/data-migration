package io.datamigration.bulkclient.internal;

import io.datamigration.bulkclient.BulkApiException;
import io.datamigration.core.format.RecordReader;
import io.datamigration.core.format.RecordStream;
import io.datamigration.core.format.csv.CsvRecordWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** CSV encoding helpers used to talk to the Salesforce Bulk API. */
final class CsvCodec {

    private CsvCodec() {}

    /** Encode a stream of records as a single CSV string with the given headers. */
    static Mono<String> encode(List<String> headers, Flux<Map<String, Object>> records) {
        return records.collectList()
                .map(
                        list -> {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            try (CsvRecordWriter writer = new CsvRecordWriter(out, headers)) {
                                for (Map<String, Object> row : list) {
                                    writer.write(row);
                                }
                            } catch (IOException e) {
                                throw new BulkApiException("Failed to encode CSV body", e);
                            }
                            return out.toString(StandardCharsets.UTF_8);
                        });
    }

    /** Parse a CSV string into a list of rows keyed by header name. */
    static List<Map<String, Object>> decode(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        try (RecordReader reader =
                RecordStream.reader(
                        RecordStream.Format.CSV,
                        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)))) {
            return reader.stream().toList();
        } catch (IOException e) {
            throw new BulkApiException("Failed to decode CSV body", e);
        }
    }
}
