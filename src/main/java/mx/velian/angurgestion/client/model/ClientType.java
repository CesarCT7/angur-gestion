package mx.velian.angurgestion.client.model;

public enum ClientType {
    PARTICULAR("Persona física"),
    EMPRESA("Persona jurídica");

    private final String label;

    ClientType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
