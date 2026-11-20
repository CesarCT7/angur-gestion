package mx.velian.angurgestion.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import mx.velian.angurgestion.auth.validation.ValidPassword;

public class InitialAdminForm {
    @NotBlank(message = "Escribe un nombre para mostrar.")
    @Size(max = 120, message = "El nombre debe tener 120 caracteres o menos.")
    private String name;

    @NotBlank(message = "Escribe un nombre de usuario.")
    @Size(min = 3, max = 80, message = "El usuario debe tener entre 3 y 80 caracteres.")
    @Pattern(
            regexp = "[A-Za-z0-9._-]+",
            message = "Usa letras sin acentos, números, punto, guion o guion bajo.")
    private String username;

    @NotBlank(message = "Escribe un correo electrónico.")
    @Email(message = "Escribe un correo válido.")
    @Size(max = 254, message = "El correo es demasiado largo.")
    private String email;

    @NotBlank(message = "Escribe una contraseña.")
    @ValidPassword
    private String password;

    @NotBlank(message = "Confirma la contraseña.")
    @Size(max = 72, message = "La confirmación es demasiado larga.")
    private String passwordConfirmation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.strip();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.strip();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }
}
