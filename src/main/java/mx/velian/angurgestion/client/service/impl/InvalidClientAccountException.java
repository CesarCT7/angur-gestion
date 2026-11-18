package mx.velian.angurgestion.client.service.impl;

public class InvalidClientAccountException extends RuntimeException {
    public InvalidClientAccountException() {
        super("La cuenta seleccionada no está disponible para vincular con este cliente.");
    }
}
