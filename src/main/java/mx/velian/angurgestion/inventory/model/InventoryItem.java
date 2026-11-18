package mx.velian.angurgestion.inventory.model;

import java.math.BigDecimal;

public record InventoryItem(
        long id,
        String code,
        String name,
        BigDecimal cost,
        BigDecimal salePrice,
        long stock) {
    public String getAvailabilityLabel() {
        return stock > 0 ? "Disponible" : "Agotado";
    }
}
