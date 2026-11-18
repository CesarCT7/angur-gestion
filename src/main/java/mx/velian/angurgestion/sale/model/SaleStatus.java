package mx.velian.angurgestion.sale.model;

public enum SaleStatus {
    REGISTRADA("Registrada"),
    CANCELADA("Cancelada");

    private final String label;
    SaleStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
