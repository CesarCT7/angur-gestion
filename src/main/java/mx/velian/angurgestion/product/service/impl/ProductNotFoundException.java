package mx.velian.angurgestion.product.service.impl;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(long id) {
        super("No se encontró el producto solicitado.");
    }
}
