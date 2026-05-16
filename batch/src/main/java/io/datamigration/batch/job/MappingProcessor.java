package io.datamigration.batch.job;

import io.datamigration.batch.model.InvalidRow;
import io.datamigration.batch.model.ProcessedRow;
import io.datamigration.batch.model.ValidRow;
import io.datamigration.core.domain.FieldMapping;
import io.datamigration.core.mapping.Mapper;
import io.datamigration.core.validation.SchemaValidator;
import io.datamigration.core.validation.TargetSchema;
import io.datamigration.core.validation.ValidationError;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.batch.item.ItemProcessor;

/**
 * Applies the configured {@link FieldMapping}s and then validates the result against the supplied
 * {@link TargetSchema}. Emits either a {@link ValidRow} (passed) or an {@link InvalidRow} (failed).
 */
public final class MappingProcessor implements ItemProcessor<Map<String, Object>, ProcessedRow> {

    private final Mapper mapper;
    private final SchemaValidator validator;
    private final List<FieldMapping> mappings;
    private final TargetSchema schema;
    private final AtomicLong counter = new AtomicLong();

    public MappingProcessor(
            Mapper mapper,
            SchemaValidator validator,
            List<FieldMapping> mappings,
            TargetSchema schema) {
        this.mapper = mapper;
        this.validator = validator;
        this.mappings = List.copyOf(mappings);
        this.schema = schema;
    }

    @Override
    public ProcessedRow process(Map<String, Object> input) {
        long rowNumber = counter.incrementAndGet();
        Map<String, Object> mapped = mapper.map(mappings, input);
        List<ValidationError> errors = validator.validate(schema, mapped);
        if (errors.isEmpty()) {
            return new ValidRow(rowNumber, mapped);
        }
        return new InvalidRow(rowNumber, mapped, errors);
    }
}
