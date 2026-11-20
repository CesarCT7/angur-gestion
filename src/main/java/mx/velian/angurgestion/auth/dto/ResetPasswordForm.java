package mx.velian.angurgestion.auth.dto;

import jakarta.validation.constraints.*;

import mx.velian.angurgestion.auth.validation.ValidPassword;

public class ResetPasswordForm {
    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "El enlace no es válido.")
    private String token;

    @NotBlank(message = "Escribe la nueva contraseña.")
    @ValidPassword
    private String password;

    @NotBlank(message = "Confirma tu contraseña.")
    @Size(max = 72, message = "La confirmación es demasiado larga.")
    private String passwordConfirmation;

    public String getToken() {
        return token;
    }

    public void setToken(String value) {
        token = value;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String value) {
        password = value;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String value) {
        passwordConfirmation = value;
    }
}
