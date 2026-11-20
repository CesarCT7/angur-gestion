package mx.velian.angurgestion.auth.dao;

import mx.velian.angurgestion.auth.model.*;

import java.util.*;

public interface UserRepository {
    Optional<UserAccount> findByIdentifier(String identifier);

    Optional<UserAccount> findById(long id);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> lockById(long id);

    List<UserAccount> findAll();

    long countUsers();

    boolean existsByUsernameOrEmail(String username, String email);

    long create(
            String username,
            String email,
            String name,
            String hash,
            UserRole role,
            boolean verified);

    void createClient(long id, String type, String name, String email, long createdBy);

    void verifyEmail(long id);

    void changePassword(long id, String hash);

    void setActive(long id, boolean active);

    void lockSetup();

    void completeSetup();
}
