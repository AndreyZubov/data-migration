package io.datamigration.core.format;

import static org.assertj.core.api.Assertions.assertThat;

import io.datamigration.core.format.RecordStream.Format;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RecordStreamRoundTripTest {

    private static List<Map<String, Object>> sampleRows() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("Name", "Ada");
        a.put("Email", "ada@example.com");
        a.put("Age", "36");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("Name", "Linus");
        b.put("Email", "linus@example.com");
        b.put("Age", "55");
        return List.of(a, b);
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    void writeReadRoundTripPreservesRowsAndOrder(Format format) throws Exception {
        List<String> headers = List.of("Name", "Email", "Age");
        List<Map<String, Object>> rows = sampleRows();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (RecordWriter writer = RecordStream.writer(format, buffer, headers)) {
            for (Map<String, Object> row : rows) {
                writer.write(row);
            }
        }

        List<Map<String, Object>> readBack;
        try (RecordReader reader =
                RecordStream.reader(format, new ByteArrayInputStream(buffer.toByteArray()));
             Stream<Map<String, Object>> stream = reader.stream()) {
            readBack = stream.toList();
        }

        assertThat(readBack).hasSize(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            for (String h : headers) {
                assertThat(readBack.get(i).get(h)).hasToString(rows.get(i).get(h).toString());
            }
        }
    }
}
