package mx.velian.angurgestion.client.service;

import java.util.List;

import mx.velian.angurgestion.client.dto.ClientForm;
import mx.velian.angurgestion.client.model.Client;
import mx.velian.angurgestion.client.model.ClientAccountOption;

public interface ClientService {
    List<Client> findAll(String search);
    ClientForm findForm(long id);
    List<ClientAccountOption> findAvailableAccounts(long currentClientId);
    void create(ClientForm form, long createdBy);
    void update(long id, ClientForm form);
    void setActive(long id, boolean active);
}
