package mx.velian.angurgestion.quotation.controller;

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
import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.quotation.dto.QuoteForm;
import mx.velian.angurgestion.quotation.model.QuoteStatus;
import mx.velian.angurgestion.quotation.service.QuoteService;
import mx.velian.angurgestion.quotation.service.impl.InvalidQuoteException;
import mx.velian.angurgestion.quotation.service.impl.QuoteNotFoundException;

@Controller
@RequestMapping("/cotizaciones")
public class QuoteController {
    private final QuoteService quotes;

    public QuoteController(QuoteService quotes) { this.quotes = quotes; }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String estado,
                        Authentication authentication, Model model) {
        UserContext user = context(authentication);
        model.addAttribute("quotes", quotes.findAll(q, estado, user.role(), user.id()));
        model.addAttribute("search", q);
        model.addAttribute("statusFilter", estado);
        model.addAttribute("canCreateQuote", canCreate(user.role()));
        model.addAttribute("canChangeStatus", canCreate(user.role()));
        model.addAttribute("pageTitle", "Cotizaciones");
        model.addAttribute("activePage", "cotizaciones");
        return "quotations/list";
    }

    @GetMapping("/nueva")
    public String createForm(Authentication authentication, Model model) {
        QuoteForm form = new QuoteForm();
        prepareForm(model, form, authentication);
        return "quotations/form";
    }

    @PostMapping("/nueva")
    public String create(@Valid @ModelAttribute("form") QuoteForm form, BindingResult bindingResult,
                         Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, form, authentication);
            return "quotations/form";
        }
        UserContext user = context(authentication);
        try {
            long id = quotes.create(form, user.id());
            redirectAttributes.addFlashAttribute("successMessage", "Cotización #" + id + " emitida como pendiente.");
            return "redirect:/cotizaciones/" + id;
        } catch (InvalidQuoteException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
        } catch (DataAccessException exception) {
            model.addAttribute("errorMessage", "No se pudo guardar la cotización. Revisa los datos y la conexión con MySQL.");
        }
        prepareForm(model, form, authentication);
        return "quotations/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        return showDetail(id, authentication, model, redirectAttributes, false);
    }

    @GetMapping("/{id}/imprimir")
    public String print(@PathVariable long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        return showDetail(id, authentication, model, redirectAttributes, true);
    }

    @PostMapping("/{id}/aceptar")
    public String accept(@PathVariable long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return updateStatus(id, QuoteStatus.ACEPTADA, authentication, redirectAttributes);
    }

    @PostMapping("/{id}/rechazar")
    public String reject(@PathVariable long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        return updateStatus(id, QuoteStatus.RECHAZADA, authentication, redirectAttributes);
    }

    private String updateStatus(long id, QuoteStatus next, Authentication authentication, RedirectAttributes redirectAttributes) {
        UserContext user = context(authentication);
        try {
            quotes.updateStatus(id, next, user.role(), user.id());
            redirectAttributes.addFlashAttribute("successMessage", next == QuoteStatus.ACEPTADA
                    ? "La cotización fue aceptada." : "La cotización fue rechazada.");
        } catch (QuoteNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontró la cotización o no tienes acceso a ella.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/cotizaciones/" + id;
    }

    private String showDetail(long id, Authentication authentication, Model model,
                              RedirectAttributes redirectAttributes, boolean print) {
        UserContext user = context(authentication);
        var quote = quotes.findById(id, user.role(), user.id());
        if (quote.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontró la cotización o no tienes acceso a ella.");
            return "redirect:/cotizaciones";
        }
        model.addAttribute("quote", quote.get());
        model.addAttribute("canCreateQuote", canCreate(user.role()));
        model.addAttribute("canChangeStatus", canCreate(user.role()) && quote.get().status() == QuoteStatus.PENDIENTE);
        model.addAttribute("pageTitle", "Cotización #" + id);
        model.addAttribute("activePage", "cotizaciones");
        return print ? "quotations/print" : "quotations/detail";
    }

    private void prepareForm(Model model, QuoteForm form, Authentication authentication) {
        model.addAttribute("form", form);
        model.addAttribute("products", quotes.findActiveProducts());
        model.addAttribute("pageTitle", "Nueva cotización");
        model.addAttribute("formTitle", "Nueva cotización");
        model.addAttribute("formAction", "/cotizaciones/nueva");
        model.addAttribute("activePage", "cotizaciones");
        model.addAttribute("canCreateQuote", canCreate(context(authentication).role()));
    }

    private static boolean canCreate(UserRole role) {
        return role == UserRole.ADMINISTRADOR || role == UserRole.VENDEDOR;
    }

    private static UserContext context(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AngurUserDetails user) {
            return new UserContext(user.getId(), user.getRole());
        }
        throw new IllegalStateException("Se requiere una sesión autenticada.");
    }

    private record UserContext(long id, UserRole role) {}
}
