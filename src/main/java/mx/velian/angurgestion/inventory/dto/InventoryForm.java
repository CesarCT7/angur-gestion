package mx.velian.angurgestion.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import mx.velian.angurgestion.inventory.model.MovementType;

public class InventoryForm {
    @NotNull(message = "Selecciona un producto.")
    private Long productId;

    @NotNull(message = "Selecciona entrada o salida.")
    private MovementType type;

    @NotNull(message = "Escribe una cantidad.")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero.")
    @Max(value = 4_294_967_295L, message = "La cantidad supera el límite permitido.")
    private Long quantity;

    @NotBlank(message = "Escribe el motivo del movimiento.")
    @Size(max = 255, message = "El motivo no puede superar 255 caracteres.")
    private String reason;

    @NotBlank
    @Pattern(regexp = "[0-9a-fA-F-]{36}", message = "Actualiza el formulario e inténtalo otra vez.")
    private String operationKey;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }
}
