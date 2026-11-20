package mx.velian.angurgestion.auth.dao;

import mx.velian.angurgestion.auth.model.AuthToken;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository {
    Optional<AuthToken> find(String hash, String purpose);

    Optional<AuthToken> lock(String hash, String purpose);

    void create(long id, String hash, String purpose, Instant expiresAt);

    void invalidate(long id, String purpose, Instant now);
}
