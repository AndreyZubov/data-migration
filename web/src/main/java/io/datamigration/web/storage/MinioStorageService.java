package io.datamigration.web.storage;

import io.datamigration.web.config.MinioConfig.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Helpers for generating presigned upload/download URLs and ensuring the bucket exists. */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient client;
    private final MinioProperties props;

    public MinioStorageService(MinioClient client, MinioProperties props) {
        this.client = client;
        this.props = props;
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists =
                    client.bucketExists(BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
                log.info("Created MinIO bucket {}", props.getBucket());
            }
        } catch (Exception e) {
            log.warn("Could not ensure MinIO bucket exists: {}", e.getMessage());
        }
    }

    public String bucket() {
        return props.getBucket();
    }

    public PresignedUpload createUploadUrl(String suffix) {
        String key = "jobs/" + UUID.randomUUID() + (suffix == null ? "" : "-" + suffix);
        try {
            String url =
                    client.getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs.builder()
                                    .method(Method.PUT)
                                    .bucket(props.getBucket())
                                    .object(key)
                                    .expiry(15, TimeUnit.MINUTES)
                                    .build());
            return new PresignedUpload(key, url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create presigned upload URL", e);
        }
    }

    public record PresignedUpload(String key, String url) {}
}
