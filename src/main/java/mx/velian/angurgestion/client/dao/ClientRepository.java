package mx.velian.angurgestion.client.dao;

import java.util.List;
import java.util.Optional;

import mx.velian.angurgestion.client.dto.ClientForm;
import mx.velian.angurgestion.client.model.Client;
import mx.velian.angurgestion.client.model.ClientAccountOption;

public interface ClientRepository {
    List<Client> findAll(String search);
    Optional<Client> findById(long id);
    List<ClientAccountOption> findAvailableAccounts(long currentClientId);
    boolean isAvailableAccount(long accountId, long currentClientId);
    boolean existsDuplicateContact(String email, String phone, long excludeClientId);
    void insert(ClientForm form, long createdBy);
    int update(long id, ClientForm form);
    int setActive(long id, boolean active);
}
