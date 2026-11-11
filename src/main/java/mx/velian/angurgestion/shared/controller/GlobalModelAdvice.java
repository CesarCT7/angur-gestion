package mx.velian.angurgestion.shared.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import mx.velian.angurgestion.auth.model.AngurUserDetails;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {
    @ModelAttribute
    public void addSignedInUser(Authentication authentication, Model model) {
        if (authentication != null && authentication.getPrincipal() instanceof AngurUserDetails user) {
            model.addAttribute("signedInName", user.getDisplayName());
            model.addAttribute("signedInRole", user.getRoleLabel());
            model.addAttribute("signedInRoleCode", user.getRole().name());
        }
    }
}
