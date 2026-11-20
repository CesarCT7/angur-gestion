package mx.velian.angurgestion.auth.dao.impl;

import mx.velian.angurgestion.auth.dao.AuthTokenRepository;
import mx.velian.angurgestion.auth.model.AuthToken;

import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class JdbcAuthTokenRepository implements AuthTokenRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT =
            "SELECT id,usuario_id,vence_en,usado_en FROM auth_tokens WHERE token_hash=? AND"
                + " proposito=?";
    private static final RowMapper<AuthToken> MAPPER =
            (rs, n) ->
                    new AuthToken(
                            rs.getLong("id"),
                            rs.getLong("usuario_id"),
                            rs.getTimestamp("vence_en").toInstant(),
                            rs.getTimestamp("usado_en") == null
                                    ? null
                                    : rs.getTimestamp("usado_en").toInstant());

    public JdbcAuthTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AuthToken> find(String hash, String purpose) {
        return jdbc.query(SELECT, MAPPER, hash, purpose).stream().findFirst();
    }

    public Optional<AuthToken> lock(String hash, String purpose) {
        return jdbc.query(SELECT + " FOR UPDATE", MAPPER, hash, purpose).stream().findFirst();
    }

    public void create(long id, String hash, String purpose, Instant expires) {
        jdbc.update(
                "INSERT INTO auth_tokens (usuario_id,token_hash,proposito,vence_en) VALUES"
                    + " (?,?,?,?)",
                id,
                hash,
                purpose,
                Timestamp.from(expires));
    }

    public void invalidate(long id, String purpose, Instant now) {
        jdbc.update(
                "UPDATE auth_tokens SET usado_en=? WHERE usuario_id=? AND proposito=? AND usado_en"
                    + " IS NULL",
                Timestamp.from(now),
                id,
                purpose);
    }
}
