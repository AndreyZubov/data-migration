package io.datamigration.web.messaging;

/** Payload published to RabbitMQ to trigger a migration job. */
public record JobMessage(String jobId, int priority) {}
