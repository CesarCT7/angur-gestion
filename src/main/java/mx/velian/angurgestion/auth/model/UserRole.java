package mx.velian.angurgestion.auth.model;

public enum UserRole {
    ADMINISTRADOR("Administrador"),
    VENDEDOR("Vendedor"),
    CLIENTE("Cliente"),
    SUPERVISOR_VENTAS("Supervisor de ventas"),
    ENCARGADO_ALMACEN("Encargado de almacén");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
