package mx.velian.angurgestion.product.service.impl;

public class DuplicateProductCodeException extends RuntimeException {
    public DuplicateProductCodeException() {
        super("Ya existe un producto con ese código.");
    }
}
