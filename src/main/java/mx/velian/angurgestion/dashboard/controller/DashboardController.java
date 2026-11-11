package mx.velian.angurgestion.dashboard.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import mx.velian.angurgestion.auth.model.AngurUserDetails;
import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.dashboard.service.DashboardService;

@Controller
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AngurUserDetails user, Model model) {
        model.addAttribute("pageTitle", "Inicio");
        model.addAttribute("activePage", "inicio");
        model.addAttribute("isClient", user.getRole() == UserRole.CLIENTE);
        model.addAttribute("showFinancials", user.getRole() == UserRole.ADMINISTRADOR
                || user.getRole() == UserRole.SUPERVISOR_VENTAS);
        model.addAttribute("showMovements", user.getRole() == UserRole.ADMINISTRADOR
                || user.getRole() == UserRole.ENCARGADO_ALMACEN);
        if (user.getRole() == UserRole.CLIENTE) {
            return "client/home";
        }

        try {
            model.addAttribute("summary", dashboardService.summarize(user.getRole()));
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudieron cargar los datos. Revisa la conexión con MySQL.");
        }
        return "dashboard/index";
    }
}
