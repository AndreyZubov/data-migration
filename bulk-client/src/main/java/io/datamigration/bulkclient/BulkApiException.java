package io.datamigration.bulkclient;

/** Thrown when a Salesforce Bulk API interaction fails after exhausted retries or on contract errors. */
public class BulkApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String errorCode;

    public BulkApiException(String message) {
        this(message, -1, null, null);
    }

    public BulkApiException(String message, Throwable cause) {
        this(message, -1, null, cause);
    }

    public BulkApiException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int statusCode() {
        return statusCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
