package mx.velian.angurgestion.auth.dto;

import jakarta.validation.constraints.NotNull;

/** Legacy request payload retained for compiling the base project's tests; no registration route exposes it. */
public class RegistrationForm extends InitialAdminForm {
    public enum ClientType { PARTICULAR, EMPRESA }

    @NotNull(message = "Selecciona el tipo de cliente.")
    private ClientType clientType = ClientType.PARTICULAR;

    public ClientType getClientType() { return clientType; }
    public void setClientType(ClientType value) { clientType = value; }
}
