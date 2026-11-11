package mx.velian.angurgestion.auth.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.model.UserAccount;
import mx.velian.angurgestion.auth.model.UserRole;

@Repository
public class JdbcUserRepository implements UserRepository {
    private static final RowMapper<UserAccount> USER_MAPPER = JdbcUserRepository::mapUser;
    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findByIdentifier(String identifier) {
        String normalized = identifier.trim().toLowerCase(Locale.ROOT);
        List<UserAccount> matches = jdbcTemplate.query("""
                SELECT id, usuario, correo, nombre, clave_hash, rol, activo
                FROM usuarios
                WHERE LOWER(usuario) = ? OR LOWER(correo) = ?
                LIMIT 1
                """, USER_MAPPER, normalized, normalized);
        return matches.stream().findFirst();
    }

    @Override
    public long countUsers() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public boolean existsByUsernameOrEmail(String username, String email) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM usuarios
                WHERE LOWER(usuario) = LOWER(?) OR LOWER(correo) = LOWER(?)
                """, Integer.class, username.trim(), email.trim());
        return count != null && count > 0;
    }

    @Override
    public void createInitialAdministrator(String username, String email, String name, String passwordHash) {
        jdbcTemplate.update("""
                INSERT INTO usuarios (usuario, correo, nombre, clave_hash, rol, activo)
                VALUES (?, ?, ?, ?, 'ADMINISTRADOR', TRUE)
                """, username.trim(), email.trim(), name.trim(), passwordHash);
    }

    private static UserAccount mapUser(ResultSet rs, int rowNumber) throws SQLException {
        return new UserAccount(
                rs.getLong("id"),
                rs.getString("usuario"),
                rs.getString("correo"),
                rs.getString("nombre"),
                rs.getString("clave_hash"),
                UserRole.valueOf(rs.getString("rol")),
                rs.getBoolean("activo"));
    }
}
