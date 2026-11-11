package mx.velian.angurgestion.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Iniciar sesión");
        return "auth/login";
    }

    @GetMapping("/acceso-denegado")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Acceso denegado");
        return "auth/access-denied";
    }
}
