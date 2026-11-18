package mx.velian.angurgestion.inventory.service.impl;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(long available) {
        super("La salida supera la existencia actual de " + available + " unidades.");
    }
}
