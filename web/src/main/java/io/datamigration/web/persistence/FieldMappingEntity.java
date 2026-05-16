package io.datamigration.web.persistence;

import io.datamigration.core.domain.TransformType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "field_mapping")
public class FieldMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private MigrationTemplateEntity template;

    @Column(name = "source_field", nullable = false)
    private String sourceField;

    @Column(name = "target_field", nullable = false)
    private String targetField;

    @Enumerated(EnumType.STRING)
    @Column(name = "transform_type")
    private TransformType transformType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transform_params", columnDefinition = "jsonb")
    private Map<String, Object> transformParams;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(nullable = false)
    private int position;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MigrationTemplateEntity getTemplate() {
        return template;
    }

    public void setTemplate(MigrationTemplateEntity template) {
        this.template = template;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public TransformType getTransformType() {
        return transformType;
    }

    public void setTransformType(TransformType transformType) {
        this.transformType = transformType;
    }

    public Map<String, Object> getTransformParams() {
        return transformParams;
    }

    public void setTransformParams(Map<String, Object> transformParams) {
        this.transformParams = transformParams;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
