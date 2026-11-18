package mx.velian.angurgestion.client.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import mx.velian.angurgestion.client.dao.ClientRepository;
import mx.velian.angurgestion.client.dto.ClientForm;
import mx.velian.angurgestion.client.model.Client;
import mx.velian.angurgestion.client.model.ClientAccountOption;

@Repository
public class JdbcClientRepository implements ClientRepository {
    private static final String SELECT_CLIENTS = """
            SELECT c.id, c.usuario_id, u.usuario, c.tipo, c.nombre_razon_social,
                   c.correo, c.telefono, c.activo
            FROM clientes c
            LEFT JOIN usuarios u ON u.id = c.usuario_id
            """;
    private static final RowMapper<Client> CLIENT_MAPPER = JdbcClientRepository::mapClient;
    private final JdbcTemplate jdbcTemplate;

    public JdbcClientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Client> findAll(String search) {
        String term = search == null ? "" : search.trim();
        if (term.isEmpty()) {
            return jdbcTemplate.query(SELECT_CLIENTS + " ORDER BY c.activo DESC, c.nombre_razon_social ASC", CLIENT_MAPPER);
        }
        String pattern = "%" + term + "%";
        return jdbcTemplate.query(SELECT_CLIENTS + """
                WHERE c.nombre_razon_social LIKE ? OR c.correo LIKE ? OR c.telefono LIKE ?
                ORDER BY c.activo DESC, c.nombre_razon_social ASC
                """, CLIENT_MAPPER, pattern, pattern, pattern);
    }

    @Override
    public Optional<Client> findById(long id) {
        return jdbcTemplate.query(SELECT_CLIENTS + " WHERE c.id = ?", CLIENT_MAPPER, id)
                .stream().findFirst();
    }

    @Override
    public List<ClientAccountOption> findAvailableAccounts(long currentClientId) {
        return jdbcTemplate.query("""
                SELECT u.id, u.usuario, u.nombre, u.correo
                FROM usuarios u
                LEFT JOIN clientes c ON c.usuario_id = u.id
                WHERE u.rol = 'CLIENTE'
                  AND ((c.id IS NULL AND u.activo = TRUE AND u.correo_verificado = TRUE) OR c.id = ?)
                ORDER BY u.nombre ASC
                """, (rs, row) -> new ClientAccountOption(
                        rs.getLong("id"), rs.getString("nombre") + " · " + rs.getString("usuario") + " · " + rs.getString("correo")),
                currentClientId);
    }

    @Override
    public boolean isAvailableAccount(long accountId, long currentClientId) {
        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM usuarios u
                LEFT JOIN clientes c ON c.usuario_id = u.id
                WHERE u.id = ? AND u.rol = 'CLIENTE'
                  AND ((u.activo = TRUE AND u.correo_verificado = TRUE AND c.id IS NULL) OR c.id = ?)
                """, Integer.class, accountId, currentClientId);
        return matches != null && matches > 0;
    }

    @Override
    public boolean existsDuplicateContact(String email, String phone, long excludeClientId) {
        Integer matches = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM clientes
                WHERE activo = TRUE AND id <> ?
                  AND ((? IS NOT NULL AND correo = ?) OR (? IS NOT NULL AND telefono = ?))
                """, Integer.class, excludeClientId, email, email, phone, phone);
        return matches != null && matches > 0;
    }

    @Override
    public void insert(ClientForm form, long createdBy) {
        jdbcTemplate.update("""
                INSERT INTO clientes (usuario_id, creado_por, tipo, nombre_razon_social, correo, telefono)
                VALUES (?, ?, ?, ?, ?, ?)
                """, form.getAccountId(), createdBy, form.getType().name(), form.getName().trim(),
                normalize(form.getEmail()), normalize(form.getPhone()));
    }

    @Override
    public int update(long id, ClientForm form) {
        return jdbcTemplate.update("""
                UPDATE clientes
                SET usuario_id = ?, tipo = ?, nombre_razon_social = ?, correo = ?, telefono = ?
                WHERE id = ?
                """, form.getAccountId(), form.getType().name(), form.getName().trim(),
                normalize(form.getEmail()), normalize(form.getPhone()), id);
    }

    @Override
    public int setActive(long id, boolean active) {
        return jdbcTemplate.update("UPDATE clientes SET activo = ? WHERE id = ? AND activo <> ?", active, id, active);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Client mapClient(ResultSet rs, int row) throws SQLException {
        long rawAccountId = rs.getLong("usuario_id");
        Long accountId = rs.wasNull() ? null : rawAccountId;
        String username = rs.getString("usuario");
        return new Client(rs.getLong("id"), accountId, username == null ? null : username,
                mx.velian.angurgestion.client.model.ClientType.valueOf(rs.getString("tipo")),
                rs.getString("nombre_razon_social"), rs.getString("correo"), rs.getString("telefono"),
                rs.getBoolean("activo"));
    }
}
