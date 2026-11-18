package mx.velian.angurgestion.quotation.model;

public enum QuoteStatus {
    BORRADOR("Borrador"),
    PENDIENTE("Pendiente"),
    EMITIDA("Emitida"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada"),
    CONVERTIDA_EN_VENTA("Convertida en venta");

    private final String label;

    QuoteStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
