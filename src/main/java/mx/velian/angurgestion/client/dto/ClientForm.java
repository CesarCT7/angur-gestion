package mx.velian.angurgestion.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import mx.velian.angurgestion.client.model.ClientType;

public class ClientForm {
    @NotNull(message = "Selecciona el tipo de cliente.")
    private ClientType type;

    @NotBlank(message = "Escribe el nombre o razón social.")
    @Size(max = 180, message = "El nombre no puede superar 180 caracteres.")
    private String name;

    @Email(message = "Escribe un correo válido.")
    @Size(max = 254, message = "El correo no puede superar 254 caracteres.")
    private String email;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres.")
    private String phone;

    private Long accountId;

    public ClientType getType() { return type; }
    public void setType(ClientType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
