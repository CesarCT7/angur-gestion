package mx.velian.angurgestion.sale.service.impl;

public class SaleNotFoundException extends RuntimeException {
    public SaleNotFoundException(long id) { super("No se encontró la venta " + id + "."); }
}
