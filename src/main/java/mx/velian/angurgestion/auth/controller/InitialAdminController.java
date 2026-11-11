package mx.velian.angurgestion.auth.controller;

import jakarta.validation.Valid;

import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import mx.velian.angurgestion.auth.dto.InitialAdminForm;
import mx.velian.angurgestion.auth.service.InitialAdminService;

@Controller
public class InitialAdminController {
    private final InitialAdminService initialAdminService;

    public InitialAdminController(InitialAdminService initialAdminService) {
        this.initialAdminService = initialAdminService;
    }

    @GetMapping("/setup")
    public String setup(Model model) {
        try {
            if (!initialAdminService.isSetupRequired()) {
                return "redirect:/login";
            }
            model.addAttribute("databaseReady", true);
        } catch (DataAccessException exception) {
            model.addAttribute("databaseReady", false);
            model.addAttribute("databaseError", "No se encontró la base de datos. Ejecuta db/schema.sql en MySQL y configura DB_URL, DB_USER y DB_PASSWORD.");
        }
        model.addAttribute("form", new InitialAdminForm());
        model.addAttribute("pageTitle", "Configuración inicial");
        return "auth/setup";
    }

    @PostMapping("/setup")
    public String create(
            @Valid @ModelAttribute("form") InitialAdminForm form,
            BindingResult bindingResult,
            Model model) {
        if (!Objects.equals(form.getPassword(), form.getPasswordConfirmation())) {
            bindingResult.rejectValue("passwordConfirmation", "mismatch", "Las contraseñas no coinciden.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("databaseReady", true);
            model.addAttribute("pageTitle", "Configuración inicial");
            return "auth/setup";
        }

        try {
            initialAdminService.createInitialAdministrator(form);
            return "redirect:/login?setup";
        } catch (IllegalStateException exception) {
            try {
                if (!initialAdminService.isSetupRequired()) {
                    return "redirect:/login";
                }
            } catch (DataAccessException ignored) {
                // Keep the configuration page visible so the operator can correct the database connection.
            }
            model.addAttribute("formError", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            bindingResult.rejectValue("username", "duplicate", "Ese usuario o correo ya está registrado.");
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar el administrador. Revisa la conexión con MySQL.");
        }

        model.addAttribute("databaseReady", true);
        model.addAttribute("pageTitle", "Configuración inicial");
        return "auth/setup";
    }
}
