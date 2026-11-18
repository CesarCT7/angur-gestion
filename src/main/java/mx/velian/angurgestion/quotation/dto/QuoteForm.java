package mx.velian.angurgestion.quotation.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class QuoteForm {
    @NotNull(message = "Selecciona un cliente.")
    private Long clientId;

    @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres.")
    private String observations;

    @Valid
    private List<QuoteLineForm> items = new ArrayList<>(List.of(new QuoteLineForm()));

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public List<QuoteLineForm> getItems() { return items; }
    public void setItems(List<QuoteLineForm> items) { this.items = items == null ? new ArrayList<>() : items; }
}
