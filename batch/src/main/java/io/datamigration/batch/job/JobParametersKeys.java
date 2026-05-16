package io.datamigration.batch.job;

/** Centralised job parameter names — keep readers, processors and writers in sync. */
public final class JobParametersKeys {

    public static final String JOB_ID = "jobId";
    public static final String BUCKET = "bucket";
    public static final String KEY = "key";
    public static final String FORMAT = "format";
    public static final String ORG_ID = "orgId";
    public static final String TARGET_OBJECT = "targetObject";
    public static final String OPERATION = "operation";
    public static final String EXTERNAL_ID_FIELD = "externalIdField";
    public static final String HEADERS_CSV = "headersCsv";

    private JobParametersKeys() {}
}
