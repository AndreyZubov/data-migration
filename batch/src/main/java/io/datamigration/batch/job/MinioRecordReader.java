package io.datamigration.batch.job;

import io.datamigration.core.format.RecordReader;
import io.datamigration.core.format.RecordStream;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.io.InputStream;
import java.util.Map;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ExecutionContext;

/**
 * Spring Batch reader backed by an object in MinIO; lazily streams the object through the
 * appropriate {@link RecordReader} implementation.
 */
public final class MinioRecordReader implements ItemReader<Map<String, Object>>, ItemStream {

    private final MinioClient client;
    private final String bucket;
    private final String key;
    private final RecordStream.Format format;
    private InputStream input;
    private RecordReader reader;
    private long rowNumber;

    public MinioRecordReader(
            MinioClient client, String bucket, String key, RecordStream.Format format) {
        this.client = client;
        this.bucket = bucket;
        this.key = key;
        this.format = format;
    }

    @Override
    public void open(ExecutionContext context) {
        try {
            input =
                    client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
            reader = RecordStream.reader(format, input);
        } catch (Exception e) {
            throw new ItemStreamException("Failed to open MinIO object " + bucket + "/" + key, e);
        }
    }

    @Override
    public Map<String, Object> read() {
        if (reader == null || !reader.hasNext()) {
            return null;
        }
        rowNumber++;
        Map<String, Object> row = reader.next();
        return new java.util.LinkedHashMap<>(row);
    }

    @Override
    public void update(ExecutionContext context) {
        context.putLong("rowNumber", rowNumber);
    }

    @Override
    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            throw new ItemStreamException("Failed to close MinIO reader", e);
        } finally {
            reader = null;
            input = null;
        }
    }

    public long rowNumber() {
        return rowNumber;
    }
}
