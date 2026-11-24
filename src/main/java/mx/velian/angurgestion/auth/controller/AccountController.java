package mx.velian.angurgestion.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import mx.velian.angurgestion.auth.dto.*;
import mx.velian.angurgestion.auth.security.AttemptLimiter;
import mx.velian.angurgestion.auth.service.AccountService;
import mx.velian.angurgestion.auth.service.impl.*;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Objects;

@Controller
public class AccountController {
    private static final String SENT =
            "Si la cuenta está activa, recibirás un enlace para verificar tu correo o restablecer"
                + " tu contraseña. Revisa también el correo no deseado.";
    private static final String UNAVAILABLE =
            "El servicio no está disponible en este momento. Inténtalo más tarde.";
    private final AccountService accounts;
    private final AttemptLimiter limiter;

    public AccountController(AccountService accounts, AttemptLimiter limiter) {
        this.accounts = accounts;
        this.limiter = limiter;
    }

    private boolean allow(HttpServletRequest request, String action, Model model) {
        if (limiter.allow(action + ":" + request.getRemoteAddr(), 5, Duration.ofMinutes(15)))
            return true;
        model.addAttribute(
                "formError",
                "Has realizado varias solicitudes. Espera 15 minutos antes de intentarlo de"
                    + " nuevo.");
        return false;
    }

    private void emailPage(Model model) {
        model.addAttribute("pageTitle", "Recuperar acceso");
        model.addAttribute("formAction", "/recuperar-acceso");
    }

    @GetMapping({"/recuperar-acceso", "/recuperar-clave", "/reenviar-verificacion"})
    public String emailForm(Model model) {
        emailPage(model);
        model.addAttribute("form", new EmailForm());
        return "auth/email-request";
    }

    @PostMapping({"/recuperar-acceso", "/recuperar-clave", "/reenviar-verificacion"})
    public String emailRequest(
            @Valid @ModelAttribute("form") EmailForm form,
            BindingResult errors,
            HttpServletRequest request,
            Model model) {
        emailPage(model);
        if (!allow(request, "recover-access", model)
                || errors.hasErrors()) return "auth/email-request";
        try {
            accounts.requestAccess(form.getEmail());
            model.addAttribute("successMessage", SENT);
        } catch (AuthOperationException error) {
            model.addAttribute("successMessage", SENT);
        } catch (DataAccessException error) {
            model.addAttribute("formError", UNAVAILABLE);
        }
        return "auth/email-request";
    }

    @GetMapping("/verificar-correo")
    public String verifyPage(@RequestParam(defaultValue = "") String token, Model model) {
        model.addAttribute("pageTitle", "Verificar correo");
        model.addAttribute("token", token);
        try {
            model.addAttribute(
                    "tokenValid", accounts.isTokenValid(token, AccountServiceImpl.VERIFY));
        } catch (DataAccessException error) {
            model.addAttribute("tokenValid", false);
            model.addAttribute("formError", UNAVAILABLE);
        }
        return "auth/verify";
    }

    @PostMapping("/verificar-correo")
    public String verify(
            @RequestParam(defaultValue = "") String token,
            HttpServletRequest request,
            Model model) {
        model.addAttribute("pageTitle", "Verificar correo");
        model.addAttribute("token", token);
        model.addAttribute("tokenValid", false);
        if (!allow(request, "consume-verify", model)) return "auth/verify";
        try {
            accounts.verifyEmail(token);
            return "redirect:/login?verified";
        } catch (AuthOperationException error) {
            model.addAttribute("formError", error.getMessage());
        } catch (DataAccessException error) {
            model.addAttribute("formError", UNAVAILABLE);
        }
        return "auth/verify";
    }

    @GetMapping("/restablecer-clave")
    public String resetPage(@RequestParam(defaultValue = "") String token, Model model) {
        var form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("form", form);
        model.addAttribute("pageTitle", "Nueva contraseña");
        try {
            model.addAttribute(
                    "tokenValid", accounts.isTokenValid(token, AccountServiceImpl.RESET));
        } catch (DataAccessException error) {
            model.addAttribute("tokenValid", false);
            model.addAttribute("formError", UNAVAILABLE);
        }
        return "auth/reset";
    }

    @PostMapping("/restablecer-clave")
    public String reset(
            @Valid @ModelAttribute("form") ResetPasswordForm form,
            BindingResult errors,
            HttpServletRequest request,
            Model model) {
        model.addAttribute("pageTitle", "Nueva contraseña");
        model.addAttribute("tokenValid", true);
        if (!Objects.equals(form.getPassword(), form.getPasswordConfirmation()))
            errors.rejectValue("passwordConfirmation", "mismatch", "Las contraseñas no coinciden.");
        if (!allow(request, "consume-reset", model) || errors.hasErrors()) return "auth/reset";
        try {
            accounts.resetPassword(form);
            return "redirect:/login?passwordChanged";
        } catch (AuthOperationException error) {
            model.addAttribute("formError", error.getMessage());
            model.addAttribute("tokenValid", false);
        } catch (DataAccessException error) {
            model.addAttribute("formError", UNAVAILABLE);
        }
        return "auth/reset";
    }
}
