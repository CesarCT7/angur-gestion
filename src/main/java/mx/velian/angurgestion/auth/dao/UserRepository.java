package mx.velian.angurgestion.auth.dao;

import java.util.Optional;

import mx.velian.angurgestion.auth.model.UserAccount;

public interface UserRepository {
    Optional<UserAccount> findByIdentifier(String identifier);

    long countUsers();

    boolean existsByUsernameOrEmail(String username, String email);

    void createInitialAdministrator(String username, String email, String name, String passwordHash);
}
