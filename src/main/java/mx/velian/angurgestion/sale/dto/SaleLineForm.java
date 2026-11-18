package mx.velian.angurgestion.sale.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SaleLineForm {
    @NotNull(message = "Selecciona un producto.")
    private Long productId;

    @NotNull(message = "Escribe la cantidad.")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero.")
    @Max(value = 4_294_967_295L, message = "La cantidad supera el límite permitido.")
    private Long quantity;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
}
