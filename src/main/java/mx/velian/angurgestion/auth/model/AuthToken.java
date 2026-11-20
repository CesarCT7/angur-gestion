package mx.velian.angurgestion.auth.model;

import java.time.Instant;

public record AuthToken(long id, long userId, Instant expiresAt, Instant usedAt) {
    public boolean usableAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
