package mx.velian.angurgestion.auth.dto;

import jakarta.validation.constraints.*;

public class AdminRequestForm {
    @NotBlank @Size(min=3, max=80) @Pattern(regexp="[A-Za-z0-9._-]+", message="Usa letras sin acentos, números, punto, guion o guion bajo.")
    private String username;
    @NotBlank @Email @Size(max=254)
    private String email;
    @NotBlank @Size(max=120)
    private String name;
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
}
