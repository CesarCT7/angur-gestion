package mx.velian.angurgestion.auth.controller;

import jakarta.validation.Valid;
import mx.velian.angurgestion.auth.dto.ResetPasswordForm;
import mx.velian.angurgestion.auth.service.impl.AdminRequestService;
import mx.velian.angurgestion.auth.service.impl.AuthOperationException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Objects;

@Controller
public class AdminApprovalController {
    private final AdminRequestService requests;
    public AdminApprovalController(AdminRequestService requests) { this.requests=requests; }

    @GetMapping("/aprobar-administrador")
    public String approval(@RequestParam(defaultValue="") String token, Model model) {
        model.addAttribute("pageTitle","Solicitud de administrador");
        model.addAttribute("token",token);
        try {
            model.addAttribute("valid",requests.approvalValid(token));
            if (Boolean.TRUE.equals(model.getAttribute("valid"))) model.addAttribute("request",requests.approvalDetails(token));
        } catch (DataAccessException e) { model.addAttribute("valid",false); model.addAttribute("formError","No pudimos consultar la solicitud. Inténtalo más tarde."); }
        return "auth/admin-approval";
    }

    @PostMapping("/aprobar-administrador/aprobar")
    public String approve(@RequestParam String token, RedirectAttributes redirect) {
        try { requests.approve(token); redirect.addFlashAttribute("successMessage","Solicitud aprobada. Enviamos al solicitante el enlace para crear su contraseña."); }
        catch (AuthOperationException e) { redirect.addFlashAttribute("formError",e.getMessage()); }
        catch (DataAccessException e) { redirect.addFlashAttribute("formError","No pudimos procesar la solicitud."); }
        return "redirect:/aprobar-administrador?token="+token;
    }

    @PostMapping("/aprobar-administrador/rechazar")
    public String reject(@RequestParam String token, RedirectAttributes redirect) {
        try { requests.reject(token); redirect.addFlashAttribute("successMessage","La solicitud fue rechazada."); }
        catch (AuthOperationException e) { redirect.addFlashAttribute("formError",e.getMessage()); }
        catch (DataAccessException e) { redirect.addFlashAttribute("formError","No pudimos procesar la solicitud."); }
        return "redirect:/aprobar-administrador?token="+token;
    }

    @GetMapping("/completar-admin")
    public String completePage(@RequestParam(defaultValue="") String token, Model model) {
        model.addAttribute("pageTitle","Completar cuenta administradora");
        try { model.addAttribute("valid",requests.registrationValid(token)); if (Boolean.TRUE.equals(model.getAttribute("valid"))) model.addAttribute("request",requests.registrationDetails(token)); }
        catch (DataAccessException e) { model.addAttribute("valid",false); model.addAttribute("formError","No pudimos consultar la invitación."); }
        ResetPasswordForm form=new ResetPasswordForm(); form.setToken(token); model.addAttribute("form",form);
        return "auth/admin-complete";
    }

    @PostMapping("/completar-admin")
    public String complete(@Valid @ModelAttribute("form") ResetPasswordForm form, BindingResult errors, Model model) {
        model.addAttribute("pageTitle","Completar cuenta administradora");
        if (!Objects.equals(form.getPassword(),form.getPasswordConfirmation())) errors.rejectValue("passwordConfirmation","mismatch","Las contraseñas no coinciden.");
        if (errors.hasErrors()) { model.addAttribute("valid",true); try { model.addAttribute("request",requests.registrationDetails(form.getToken())); } catch (RuntimeException ignored) { model.addAttribute("valid",false); } return "auth/admin-complete"; }
        try { requests.complete(form); return "redirect:/login?adminCreated"; }
        catch (AuthOperationException e) { model.addAttribute("valid",false); model.addAttribute("formError",e.getMessage()); }
        catch (DataAccessException e) { model.addAttribute("valid",true); model.addAttribute("formError","No pudimos crear la cuenta. Inténtalo más tarde."); try { model.addAttribute("request",requests.registrationDetails(form.getToken())); } catch (RuntimeException ignored) { model.addAttribute("valid",false); } }
        return "auth/admin-complete";
    }
}
