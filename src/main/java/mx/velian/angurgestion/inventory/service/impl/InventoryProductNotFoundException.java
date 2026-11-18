package mx.velian.angurgestion.inventory.service.impl;

public class InventoryProductNotFoundException extends RuntimeException {
    public InventoryProductNotFoundException(long id) {
        super("El producto " + id + " no existe o está inactivo.");
    }
}
