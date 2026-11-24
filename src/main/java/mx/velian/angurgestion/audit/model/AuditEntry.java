package mx.velian.angurgestion.audit.model;

import java.time.LocalDateTime;

public record AuditEntry(long id, LocalDateTime occurredAt, String actor, String entityType,
                         String entityKey, String action, String description) {}
