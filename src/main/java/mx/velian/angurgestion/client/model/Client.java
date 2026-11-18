package mx.velian.angurgestion.client.model;

public record Client(
        long id,
        Long accountId,
        String accountLabel,
        ClientType type,
        String name,
        String email,
        String phone,
        boolean active) {}
