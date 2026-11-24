package mx.velian.angurgestion.sale.controller;

import java.util.UUID;

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
import mx.velian.angurgestion.quotation.service.QuoteService;
import mx.velian.angurgestion.sale.dto.SaleForm;
import mx.velian.angurgestion.sale.dto.SaleLineForm;
import mx.velian.angurgestion.sale.service.SaleService;
import mx.velian.angurgestion.sale.service.impl.InsufficientSaleStockException;
import mx.velian.angurgestion.sale.service.impl.InvalidSaleException;
import mx.velian.angurgestion.sale.service.impl.QuoteAlreadySoldException;

@Controller
@RequestMapping("/ventas")
public class SaleController {
    private final SaleService sales;
    private final QuoteService quotes;

    public SaleController(SaleService sales, QuoteService quotes) { this.sales = sales; this.quotes = quotes; }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String estado,
                        Authentication authentication, Model model) {
        UserContext user = context(authentication);
        model.addAttribute("sales", sales.findAll(q, estado, user.role(), user.id()));
        model.addAttribute("search", q);
        model.addAttribute("statusFilter", estado);
        model.addAttribute("pageTitle", "Ventas");
        model.addAttribute("activePage", "ventas");
        model.addAttribute("canCreateSale", canCreate(user.role()));
        return "sales/list";
    }

    @GetMapping("/nueva")
    public String form(Authentication auth, Model model) {
        SaleForm form = new SaleForm(); form.setOperationKey(UUID.randomUUID().toString());
        prepare(model, form, auth);
        return "sales/form";
    }

    @GetMapping("/nueva/cotizacion/{quoteId}")
    public String fromQuote(@PathVariable long quoteId, Authentication auth, Model model, RedirectAttributes flash) {
        UserContext user = context(auth);
        var quote = quotes.findAcceptedForSale(quoteId, user.role(), user.id());
        if (quote.isEmpty() || quote.get().saleId() != null) {
            flash.addFlashAttribute("errorMessage", "La cotización no está disponible para registrar una venta.");
            return "redirect:/cotizaciones/" + quoteId;
        }
        SaleForm form = new SaleForm(); form.setQuoteId(quoteId); form.setOperationKey(UUID.randomUUID().toString());
        prepare(model, form, auth); model.addAttribute("quote", quote.get()); model.addAttribute("quoteMode", true);
        model.addAttribute("pageTitle", "Confirmar venta de cotización #" + quoteId);
        return "sales/form";
    }

    @PostMapping("/nueva")
    public String create(@Valid @ModelAttribute("form") SaleForm form, BindingResult errors, Authentication auth,
                        Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            prepare(model, form, auth);
            if (form.getQuoteId() != null) quotes.findAcceptedForSale(form.getQuoteId(), context(auth).role(), context(auth).id())
                    .ifPresent(quote -> model.addAttribute("quote", quote));
            model.addAttribute("quoteMode", form.getQuoteId() != null);
            return "sales/form";
        }
        UserContext user = context(auth);
        try {
            var result = sales.create(form, user.id(), user.role());
            flash.addFlashAttribute("successMessage", result.alreadyProcessed()
                    ? "La operación ya estaba registrada; se mostró la venta existente."
                    : "Venta #" + result.id() + " registrada y descontada del inventario.");
            return "redirect:/ventas/" + result.id();
        } catch (InsufficientSaleStockException | InvalidSaleException | QuoteAlreadySoldException e) {
            model.addAttribute("errorMessage", e.getMessage());
        } catch (DataAccessException e) {
            model.addAttribute("errorMessage", "No se pudo registrar la venta. No se aplicaron cambios parciales.");
        }
        prepare(model, form, auth);
        if (form.getQuoteId() != null) quotes.findAcceptedForSale(form.getQuoteId(), user.role(), user.id())
                .ifPresent(quote -> model.addAttribute("quote", quote));
        model.addAttribute("quoteMode", form.getQuoteId() != null);
        return "sales/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Authentication auth, Model model, RedirectAttributes flash) {
        UserContext user = context(auth);
        var sale = sales.findById(id, user.role(), user.id());
        if (sale.isEmpty()) {
            flash.addFlashAttribute("errorMessage", "No se encontró la venta o no tienes acceso.");
            return "redirect:/ventas";
        }
        model.addAttribute("sale", sale.get());
        model.addAttribute("showCosts", user.role() == UserRole.ADMINISTRADOR || user.role() == UserRole.SUPERVISOR_VENTAS);
        model.addAttribute("pageTitle", "Venta #" + id); model.addAttribute("activePage", "ventas");
        return "sales/detail";
    }

    private void prepare(Model model, SaleForm form, Authentication auth) {
        if (form.getQuoteId() == null) {
            if (form.getItems().isEmpty()) {
                form.getItems().add(new SaleLineForm());
            }
            model.addAttribute("products", sales.findActiveProducts());
        }
        model.addAttribute("form", form);
        model.addAttribute("pageTitle", "Nueva venta");
        model.addAttribute("activePage", "ventas"); model.addAttribute("quoteMode", form.getQuoteId() != null);
        model.addAttribute("canCreateSale", canCreate(context(auth).role()));
    }
    private static boolean canCreate(UserRole role) { return role == UserRole.ADMINISTRADOR || role == UserRole.VENDEDOR; }
    private static UserContext context(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof AngurUserDetails user) return new UserContext(user.getId(), user.getRole());
        throw new IllegalStateException("Se requiere una sesión autenticada.");
    }
    private record UserContext(long id, UserRole role) {}
}
