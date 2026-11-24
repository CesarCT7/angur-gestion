package mx.velian.angurgestion.auth.dao;

import mx.velian.angurgestion.auth.model.*;

import java.util.*;

public interface UserRepository {
    Optional<UserAccount> findByIdentifier(String identifier);

    Optional<UserAccount> findById(long id);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> lockById(long id);

    Optional<UserAccount> findPrincipalAdministrator();

    boolean isPrincipalAdministrator(long id);

    void setPrincipalAdministrator(long id);

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

    void verifyEmail(long id);

    void changePassword(long id, String hash);

    void setActive(long id, boolean active);

    void lockSetup();

    void completeSetup(long administratorId);
}
