package mx.velian.angurgestion.sale.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class SaleForm {
    @Min(value = 1, message = "La cotización seleccionada no es válida.")
    private Long quoteId;

    @Pattern(regexp = "[0-9a-fA-F-]{36}", message = "Actualiza el formulario e inténtalo otra vez.")
    private String operationKey;

    @Valid
    private List<SaleLineForm> items = new ArrayList<>();

    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getOperationKey() { return operationKey; }
    public void setOperationKey(String operationKey) { this.operationKey = operationKey; }
    public List<SaleLineForm> getItems() { return items; }
    public void setItems(List<SaleLineForm> items) { this.items = items == null ? new ArrayList<>() : items; }
}
