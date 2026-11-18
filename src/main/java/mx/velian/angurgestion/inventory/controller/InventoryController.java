package mx.velian.angurgestion.inventory.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mx.velian.angurgestion.auth.model.AngurUserDetails;
import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.inventory.dto.InventoryForm;
import mx.velian.angurgestion.inventory.model.MovementType;
import mx.velian.angurgestion.inventory.service.InventoryService;
import mx.velian.angurgestion.inventory.service.impl.InsufficientStockException;
import mx.velian.angurgestion.inventory.service.impl.InventoryProductNotFoundException;

@Controller
@RequestMapping("/inventario")
public class InventoryController {
    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q, Authentication authentication, Model model) {
        boolean canManage = canManageInventory(authentication);
        model.addAttribute("items", inventory.findItems(q));
        model.addAttribute("search", q);
        model.addAttribute("canManageInventory", canManage);
        model.addAttribute("canViewCost", canManage);
        if (canManage) model.addAttribute("movements", inventory.findRecentMovements(q, 100));
        model.addAttribute("pageTitle", "Productos e inventario");
        model.addAttribute("activePage", "inventario");
        return "inventory/list";
    }

    @GetMapping("/movimiento")
    public String movementForm(@RequestParam(defaultValue = "ENTRADA") String tipo, Model model) {
        InventoryForm form = new InventoryForm();
        try {
            form.setType(MovementType.valueOf(tipo.toUpperCase()));
        } catch (IllegalArgumentException exception) {
            form.setType(MovementType.ENTRADA);
        }
        form.setOperationKey(UUID.randomUUID().toString());
        prepareForm(model, form);
        return "inventory/movement-form";
    }

    @PostMapping("/movimiento")
    public String record(
            @Valid @ModelAttribute("form") InventoryForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form);
            return "inventory/movement-form";
        }
        try {
            var result = inventory.record(form, userId(authentication));
            redirectAttributes.addFlashAttribute("successMessage", result.alreadyProcessed()
                    ? "Esta operación ya había sido registrada; no se volvió a modificar la existencia."
                    : "Movimiento registrado. Existencia resultante: " + result.resultingStock() + " unidades.");
            return "redirect:/inventario";
        } catch (InsufficientStockException exception) {
            bindingResult.rejectValue("quantity", "insufficient", exception.getMessage());
        } catch (InventoryProductNotFoundException exception) {
            bindingResult.rejectValue("productId", "unavailable", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("quantity", "range", exception.getMessage());
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar el movimiento. Revisa la conexión con MySQL e inténtalo de nuevo.");
        }
        prepareForm(model, form);
        return "inventory/movement-form";
    }

    private void prepareForm(Model model, InventoryForm form) {
        model.addAttribute("form", form);
        model.addAttribute("items", inventory.findItems(""));
        model.addAttribute("formAction", "/inventario/movimiento");
        model.addAttribute("pageTitle", "Registrar movimiento");
        model.addAttribute("activePage", "inventario");
    }

    private static boolean canManageInventory(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AngurUserDetails user)) return false;
        return user.getRole() == UserRole.ADMINISTRADOR || user.getRole() == UserRole.ENCARGADO_ALMACEN;
    }

    private static long userId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AngurUserDetails user) return user.getId();
        throw new IllegalStateException("No hay una sesión autenticada para registrar el movimiento.");
    }
}
