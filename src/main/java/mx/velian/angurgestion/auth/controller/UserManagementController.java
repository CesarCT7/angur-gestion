package mx.velian.angurgestion.auth.controller;

import jakarta.validation.Valid;

import mx.velian.angurgestion.auth.dto.AdminRequestForm;
import mx.velian.angurgestion.auth.model.*;
import mx.velian.angurgestion.auth.service.AccountService;
import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.service.impl.AuthOperationException;
import mx.velian.angurgestion.auth.service.impl.AdminRequestService;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/usuarios")
public class UserManagementController {
    private final AccountService accounts;
    private final UserRepository users;
    private final AdminRequestService requestService;

    public UserManagementController(AccountService accounts, UserRepository users, AdminRequestService requestService) {
        this.accounts = accounts;
        this.users = users;
        this.requestService = requestService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AngurUserDetails actor, Model model) {
        model.addAttribute("pageTitle", "Usuarios");
        model.addAttribute("activePage", "usuarios");
        try {
            model.addAttribute("accounts", accounts.accounts());
            model.addAttribute("principalId", users.findPrincipalAdministrator().map(UserAccount::id).orElse(null));
            model.addAttribute("canTransferPrincipal", accounts.isPrincipal(actor.getId()));
        } catch (DataAccessException error) {
            model.addAttribute("formError", "No pudimos cargar las cuentas. Inténtalo más tarde.");
        }
        return "auth/users";
    }

    private void prepare(Model model) {
        model.addAttribute("pageTitle", "Solicitar administrador");
        model.addAttribute("activePage", "usuarios");
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        prepare(model);
        model.addAttribute("form", new AdminRequestForm());
        return "auth/account-form";
    }

    @PostMapping("/nuevo")
    public String create(
            @Valid @ModelAttribute("form") AdminRequestForm form,
            BindingResult errors,
            @AuthenticationPrincipal AngurUserDetails actor,
            Model model,
            RedirectAttributes redirect) {
        prepare(model);
        if (errors.hasErrors()) return "auth/account-form";
        try {
            // Request service dispatches to the principal administrator for approval.
            requestService.request(form, actor.getId());
            redirect.addFlashAttribute(
                    "successMessage",
                    "Solicitud enviada al correo del administrador principal.");
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

    @PostMapping("/{id}/principal")
    public String principal(@PathVariable long id, @AuthenticationPrincipal AngurUserDetails actor, RedirectAttributes redirect) {
        try {
            accounts.setPrincipal(id, actor.getId());
            redirect.addFlashAttribute("successMessage", "Se actualizó el administrador principal.");
        } catch (AuthOperationException error) {
            redirect.addFlashAttribute("formError", error.getMessage());
        } catch (DataAccessException error) {
            redirect.addFlashAttribute("formError", "No pudimos cambiar el administrador principal.");
        }
        return "redirect:/usuarios";
    }
}
