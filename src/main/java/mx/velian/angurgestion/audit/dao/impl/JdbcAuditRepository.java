package mx.velian.angurgestion.audit.dao.impl;

import mx.velian.angurgestion.audit.dao.AuditRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditRepository implements AuditRepository {
    private final JdbcTemplate jdbc;
    public JdbcAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(long actorId, String entityType, String entityKey, String action, String description) {
        jdbc.update("INSERT INTO auditoria_eventos(usuario_id, entidad_tipo, entidad_clave, accion, descripcion) VALUES (?,?,?,?,?)",
                actorId, entityType, entityKey, action, description);
    }
}
