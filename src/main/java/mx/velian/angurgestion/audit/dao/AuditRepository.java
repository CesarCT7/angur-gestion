package mx.velian.angurgestion.audit.dao;

public interface AuditRepository {
    void insert(long actorId, String entityType, String entityKey, String action, String description);
}
