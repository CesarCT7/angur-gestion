package mx.velian.angurgestion.inventory.model;

public enum MovementType {
    ENTRADA("Entrada"),
    SALIDA("Salida");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
