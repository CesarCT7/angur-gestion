package mx.velian.angurgestion.auth.dto;

import jakarta.validation.constraints.NotNull;

import mx.velian.angurgestion.auth.model.UserRole;

public class AccountForm extends RegistrationForm {
    @NotNull(message = "Selecciona un perfil.")
    private UserRole role = UserRole.VENDEDOR;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole value) {
        role = value;
    }
}
