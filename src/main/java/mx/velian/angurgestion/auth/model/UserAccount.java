package mx.velian.angurgestion.auth.model;

public record UserAccount(
        long id,
        String username,
        String email,
        String name,
        String passwordHash,
        UserRole role,
        boolean active) {
}
