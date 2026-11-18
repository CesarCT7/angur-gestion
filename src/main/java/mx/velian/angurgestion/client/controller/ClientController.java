package mx.velian.angurgestion.client.controller;

import jakarta.validation.Valid;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mx.velian.angurgestion.auth.model.AngurUserDetails;
import mx.velian.angurgestion.client.dto.ClientForm;
import mx.velian.angurgestion.client.service.ClientService;
import mx.velian.angurgestion.client.service.impl.ClientNotFoundException;
import mx.velian.angurgestion.client.service.impl.DuplicateClientContactException;
import mx.velian.angurgestion.client.service.impl.InvalidClientAccountException;

@Controller
@RequestMapping("/clientes")
public class ClientController {
    private final ClientService clients;

    public ClientController(ClientService clients) {
        this.clients = clients;
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("clients", clients.findAll(q));
        model.addAttribute("search", q);
        model.addAttribute("pageTitle", "Clientes");
        model.addAttribute("activePage", "clientes");
        return "clients/list";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        prepareForm(model, new ClientForm(), 0, "Nuevo cliente");
        return "clients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ClientForm form, BindingResult bindingResult,
                         Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, 0, "Nuevo cliente");
            return "clients/form";
        }
        try {
            clients.create(form, userId(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Cliente registrado correctamente.");
            return "redirect:/clientes";
        } catch (InvalidClientAccountException exception) {
            bindingResult.rejectValue("accountId", "unavailable", exception.getMessage());
        } catch (DuplicateClientContactException exception) {
            bindingResult.reject("duplicateContact", exception.getMessage());
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar el cliente. Revisa los datos y la conexión con MySQL.");
        }
        prepareForm(model, form, 0, "Nuevo cliente");
        return "clients/form";
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            prepareForm(model, clients.findForm(id), id, "Editar cliente");
            return "clients/form";
        } catch (ClientNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "El cliente solicitado ya no está disponible.");
            return "redirect:/clientes";
        }
    }

    @PostMapping("/{id}/editar")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") ClientForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, id, "Editar cliente");
            return "clients/form";
        }
        try {
            clients.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cambios guardados correctamente.");
            return "redirect:/clientes";
        } catch (InvalidClientAccountException exception) {
            bindingResult.rejectValue("accountId", "unavailable", exception.getMessage());
        } catch (DuplicateClientContactException exception) {
            bindingResult.reject("duplicateContact", exception.getMessage());
        } catch (ClientNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "El cliente solicitado ya no está disponible.");
            return "redirect:/clientes";
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar el cliente. Revisa los datos y la conexión con MySQL.");
        }
        prepareForm(model, form, id, "Editar cliente");
        return "clients/form";
    }

    @PostMapping("/{id}/desactivar")
    public String deactivate(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return setActive(id, false, redirectAttributes);
    }

    @PostMapping("/{id}/activar")
    public String reactivate(@PathVariable long id, RedirectAttributes redirectAttributes) {
        return setActive(id, true, redirectAttributes);
    }

    private String setActive(long id, boolean active, RedirectAttributes redirectAttributes) {
        try {
            clients.setActive(id, active);
            redirectAttributes.addFlashAttribute("successMessage", active ? "Cliente reactivado." : "Cliente desactivado; su historial se conserva.");
        } catch (ClientNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "El cliente solicitado ya no está disponible.");
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo actualizar el estado del cliente. Inténtalo de nuevo.");
        }
        return "redirect:/clientes";
    }

    private void prepareForm(Model model, ClientForm form, long clientId, String title) {
        model.addAttribute("form", form);
        model.addAttribute("accounts", clients.findAvailableAccounts(clientId));
        model.addAttribute("formAction", clientId == 0 ? "/clientes" : "/clientes/" + clientId + "/editar");
        model.addAttribute("pageTitle", title);
        model.addAttribute("formTitle", title);
        model.addAttribute("activePage", "clientes");
    }

    private static long userId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AngurUserDetails user) return user.getId();
        throw new IllegalStateException("No hay una sesión autenticada para registrar el cliente.");
    }
}
