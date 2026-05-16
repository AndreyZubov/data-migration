package io.datamigration.web.batch;

import io.datamigration.batch.model.InvalidRow;
import io.datamigration.batch.spi.ErrorSink;
import io.datamigration.core.validation.ValidationError;
import io.datamigration.web.persistence.ErrorRecordEntity;
import io.datamigration.web.persistence.ErrorRecordRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** {@link ErrorSink} backed by JPA — explodes each invalid row into one row per validation error. */
@Component
public class JpaErrorSink implements ErrorSink {

    private final ErrorRecordRepository repository;

    public JpaErrorSink(ErrorRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void persist(String jobId, List<InvalidRow> invalidRows) {
        List<ErrorRecordEntity> entities = new ArrayList<>();
        Instant now = Instant.now();
        for (InvalidRow row : invalidRows) {
            for (ValidationError error : row.errors()) {
                ErrorRecordEntity entity = new ErrorRecordEntity();
                entity.setJobId(jobId);
                entity.setRowNumber(row.rowNumber());
                entity.setField(error.field());
                entity.setErrorCode(error.code());
                entity.setMessage(error.message());
                entity.setRowSnapshot(row.data());
                entity.setCreatedAt(now);
                entities.add(entity);
            }
        }
        repository.saveAll(entities);
    }
}
