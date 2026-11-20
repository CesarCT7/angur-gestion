package mx.velian.angurgestion.auth.controller;

import jakarta.validation.Valid;

import mx.velian.angurgestion.auth.dto.AccountForm;
import mx.velian.angurgestion.auth.model.*;
import mx.velian.angurgestion.auth.service.AccountService;
import mx.velian.angurgestion.auth.service.impl.AuthOperationException;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
@RequestMapping("/usuarios")
public class UserManagementController {
    private final AccountService accounts;

    public UserManagementController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Usuarios");
        model.addAttribute("activePage", "usuarios");
        try {
            model.addAttribute("accounts", accounts.accounts());
        } catch (DataAccessException error) {
            model.addAttribute("formError", "No pudimos cargar las cuentas. Inténtalo más tarde.");
        }
        return "auth/users";
    }

    private void prepare(Model model) {
        model.addAttribute("pageTitle", "Crear cuenta");
        model.addAttribute("activePage", "usuarios");
        model.addAttribute("roles", UserRole.values());
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        prepare(model);
        model.addAttribute("form", new AccountForm());
        return "auth/account-form";
    }

    @PostMapping("/nuevo")
    public String create(
            @Valid @ModelAttribute("form") AccountForm form,
            BindingResult errors,
            @AuthenticationPrincipal AngurUserDetails actor,
            Model model,
            RedirectAttributes redirect) {
        prepare(model);
        if (!Objects.equals(form.getPassword(), form.getPasswordConfirmation()))
            errors.rejectValue("passwordConfirmation", "mismatch", "Las contraseñas no coinciden.");
        if (errors.hasErrors()) return "auth/account-form";
        try {
            accounts.createAccount(form, actor.getId());
            redirect.addFlashAttribute(
                    "successMessage",
                    "Cuenta creada. La persona debe verificar su correo antes de entrar.");
            return "redirect:/usuarios";
        } catch (AuthOperationException error) {
            model.addAttribute("formError", error.getMessage());
        } catch (DataAccessException error) {
            model.addAttribute("formError", "No pudimos guardar la cuenta. Inténtalo más tarde.");
        }
        return "auth/account-form";
    }

    @PostMapping("/{id}/estado")
    public String active(
            @PathVariable long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal AngurUserDetails actor,
            RedirectAttributes redirect) {
        try {
            accounts.setActive(id, active, actor.getId());
            redirect.addFlashAttribute(
                    "successMessage",
                    active
                            ? "La cuenta se activó."
                            : "La cuenta se desactivó y sus sesiones se cerraron.");
        } catch (AuthOperationException error) {
            redirect.addFlashAttribute("formError", error.getMessage());
        } catch (DataAccessException error) {
            redirect.addFlashAttribute(
                    "formError", "No pudimos cambiar el estado. Inténtalo más tarde.");
        }
        return "redirect:/usuarios";
    }
}
