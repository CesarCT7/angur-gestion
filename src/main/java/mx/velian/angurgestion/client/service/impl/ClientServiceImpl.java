package mx.velian.angurgestion.client.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.velian.angurgestion.client.dao.ClientRepository;
import mx.velian.angurgestion.client.dto.ClientForm;
import mx.velian.angurgestion.client.model.Client;
import mx.velian.angurgestion.client.model.ClientAccountOption;
import mx.velian.angurgestion.client.service.ClientService;

@Service
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clients;

    public ClientServiceImpl(ClientRepository clients) {
        this.clients = clients;
    }

    @Override
    public List<Client> findAll(String search) {
        return clients.findAll(search);
    }

    @Override
    public ClientForm findForm(long id) {
        Client client = clients.findById(id).orElseThrow(() -> new ClientNotFoundException(id));
        ClientForm form = new ClientForm();
        form.setType(client.type());
        form.setName(client.name());
        form.setEmail(client.email());
        form.setPhone(client.phone());
        form.setAccountId(client.accountId());
        return form;
    }

    @Override
    public List<ClientAccountOption> findAvailableAccounts(long currentClientId) {
        return clients.findAvailableAccounts(currentClientId);
    }

    @Override
    @Transactional
    public void create(ClientForm form, long createdBy) {
        validateAccount(form.getAccountId(), 0);
        validateContact(form, 0);
        clients.insert(form, createdBy);
    }

    @Override
    @Transactional
    public void update(long id, ClientForm form) {
        if (clients.findById(id).isEmpty()) throw new ClientNotFoundException(id);
        validateAccount(form.getAccountId(), id);
        validateContact(form, id);
        if (clients.update(id, form) == 0) throw new ClientNotFoundException(id);
    }

    @Override
    @Transactional
    public void setActive(long id, boolean active) {
        if (clients.setActive(id, active) == 0 && clients.findById(id).isEmpty()) {
            throw new ClientNotFoundException(id);
        }
    }

    private void validateAccount(Long accountId, long currentClientId) {
        if (accountId != null && !clients.isAvailableAccount(accountId, currentClientId)) {
            throw new InvalidClientAccountException();
        }
    }

    private void validateContact(ClientForm form, long currentClientId) {
        String email = normalize(form.getEmail());
        String phone = normalize(form.getPhone());
        if ((email != null || phone != null)
                && clients.existsDuplicateContact(email, phone, currentClientId)) {
            throw new DuplicateClientContactException();
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
