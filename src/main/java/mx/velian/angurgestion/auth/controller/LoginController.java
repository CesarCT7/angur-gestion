package mx.velian.angurgestion.auth.controller;

import mx.velian.angurgestion.auth.model.AngurUserDetails;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof AngurUserDetails)
            return "redirect:/";
        model.addAttribute("pageTitle", "Iniciar sesión");
        return "auth/login";
    }

    @GetMapping("/acceso-denegado")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Acceso denegado");
        return "auth/access-denied";
    }
}
