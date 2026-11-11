package mx.velian.angurgestion.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductForm {
    @NotBlank(message = "Escribe el código del producto.")
    @Size(max = 80, message = "El código debe tener 80 caracteres o menos.")
    private String code;

    @NotBlank(message = "Escribe el nombre del producto.")
    @Size(max = 160, message = "El nombre debe tener 160 caracteres o menos.")
    private String name;

    @Size(max = 5000, message = "La descripción debe tener 5000 caracteres o menos.")
    private String description;

    @NotNull(message = "Escribe el costo.")
    @DecimalMin(value = "0.00", message = "El costo no puede ser negativo.")
    @Digits(integer = 11, fraction = 2, message = "El costo admite hasta 11 enteros y 2 decimales.")
    private BigDecimal cost;

    @NotNull(message = "Escribe el precio de venta.")
    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo.")
    @Digits(integer = 11, fraction = 2, message = "El precio admite hasta 11 enteros y 2 decimales.")
    private BigDecimal salePrice;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
}
