package mx.velian.angurgestion.product.model;

import java.math.BigDecimal;

public record Product(
        long id,
        String code,
        String name,
        String description,
        BigDecimal cost,
        BigDecimal salePrice,
        long stock,
        boolean active) {
    public String getAvailabilityLabel() {
        return stock > 0 ? "Disponible" : "Agotado";
    }
}
