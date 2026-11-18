package mx.velian.angurgestion.client.service.impl;

public class DuplicateClientContactException extends RuntimeException {
    public DuplicateClientContactException() {
        super("Ya existe un cliente activo con ese correo o teléfono. Busca el registro antes de crear otro.");
    }
}
