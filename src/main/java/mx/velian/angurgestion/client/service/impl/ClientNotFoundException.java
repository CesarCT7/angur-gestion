package mx.velian.angurgestion.client.service.impl;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(long id) {
        super("No se encontró el cliente " + id + ".");
    }
}
