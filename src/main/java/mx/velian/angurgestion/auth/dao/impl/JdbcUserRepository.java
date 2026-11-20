package mx.velian.angurgestion.auth.dao.impl;

import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.model.*;

import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
public class JdbcUserRepository implements UserRepository {
    private static final String SELECT =
            "SELECT id, usuario, correo, nombre, clave_hash, rol, activo, correo_verificado,"
                + " version_sesion FROM usuarios ";
    private static final RowMapper<UserAccount> MAPPER =
            (rs, n) ->
                    new UserAccount(
                            rs.getLong("id"),
                            rs.getString("usuario"),
                            rs.getString("correo"),
                            rs.getString("nombre"),
                            rs.getString("clave_hash"),
                            UserRole.valueOf(rs.getString("rol")),
                            rs.getBoolean("activo"),
                            rs.getBoolean("correo_verificado"),
                            rs.getLong("version_sesion"));
    private final JdbcTemplate jdbc;

    public JdbcUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Optional<UserAccount> one(String sql, Object... args) {
        return jdbc.query(sql, MAPPER, args).stream().findFirst();
    }

    public Optional<UserAccount> findByIdentifier(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return one(
                SELECT + "WHERE LOWER(usuario)=? OR LOWER(correo)=? LIMIT 1",
                normalized,
                normalized);
    }

    public Optional<UserAccount> findByEmail(String email) {
        return one(SELECT + "WHERE LOWER(correo)=?", email.trim().toLowerCase(Locale.ROOT));
    }

    public Optional<UserAccount> findById(long id) {
        return one(SELECT + "WHERE id=?", id);
    }

    public Optional<UserAccount> lockById(long id) {
        return one(SELECT + "WHERE id=? FOR UPDATE", id);
    }

    public List<UserAccount> findAll() {
        return jdbc.query(SELECT + "ORDER BY nombre,id", MAPPER);
    }

    public long countUsers() {
        return Objects.requireNonNull(
                jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class));
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        Integer n =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM usuarios WHERE usuario=? OR correo=? OR usuario=? OR"
                            + " correo=?",
                        Integer.class,
                        username.trim(),
                        email.trim(),
                        email.trim(),
                        username.trim());
        return n != null && n > 0;
    }

    public long create(
            String username,
            String email,
            String name,
            String hash,
            UserRole role,
            boolean verified) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement st =
                            connection.prepareStatement(
                                    "INSERT INTO usuarios"
                                        + " (usuario,correo,nombre,clave_hash,rol,correo_verificado)"
                                        + " VALUES (?,?,?,?,?,?)",
                                    Statement.RETURN_GENERATED_KEYS);
                    st.setString(1, username.trim().toLowerCase(Locale.ROOT));
                    st.setString(2, email.trim().toLowerCase(Locale.ROOT));
                    st.setString(3, name.trim());
                    st.setString(4, hash);
                    st.setString(5, role.name());
                    st.setBoolean(6, verified);
                    return st;
                },
                keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    public void createClient(long id, String type, String name, String email, long author) {
        jdbc.update(
                "INSERT INTO clientes (usuario_id,creado_por,tipo,nombre_razon_social,correo)"
                    + " VALUES (?,?,?,?,?)",
                id,
                author,
                type,
                name.trim(),
                email.trim().toLowerCase(Locale.ROOT));
    }

    public void verifyEmail(long id) {
        jdbc.update(
                "UPDATE usuarios SET correo_verificado=TRUE,version_sesion=version_sesion+1 WHERE"
                    + " id=?",
                id);
    }

    public void changePassword(long id, String hash) {
        jdbc.update(
                "UPDATE usuarios SET clave_hash=?,version_sesion=version_sesion+1 WHERE id=?",
                hash,
                id);
    }

    public void setActive(long id, boolean active) {
        jdbc.update(
                "UPDATE usuarios SET activo=?,version_sesion=version_sesion+1 WHERE id=?",
                active,
                id);
    }

    public void lockSetup() {
        jdbc.queryForObject("SELECT id FROM auth_setup WHERE id=1 FOR UPDATE", Integer.class);
    }

    public void completeSetup() {
        jdbc.update("UPDATE auth_setup SET completado=TRUE WHERE id=1");
    }
}
