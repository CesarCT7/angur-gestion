package mx.velian.angurgestion.auth.dto;

import jakarta.validation.constraints.*;

public class EmailForm {
    @NotBlank(message = "Escribe tu correo.")
    @Email(message = "Escribe un correo válido.")
    @Size(max = 254, message = "El correo es demasiado largo.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        email = value == null ? null : value.strip();
    }
}
