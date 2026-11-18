package mx.velian.angurgestion.sale.service.impl;

public class InsufficientSaleStockException extends RuntimeException {
    public InsufficientSaleStockException(String product, long available) {
        super("Existencia insuficiente de " + product + ". Disponible: " + available + " unidades.");
    }
}
