package mx.velian.angurgestion.product.controller;

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
import mx.velian.angurgestion.product.dto.ProductForm;
import mx.velian.angurgestion.product.service.ProductService;
import mx.velian.angurgestion.product.service.impl.DuplicateProductCodeException;
import mx.velian.angurgestion.product.service.impl.ProductNotFoundException;

@Controller
@RequestMapping("/productos")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, Authentication authentication, Model model) {
        model.addAttribute("products", productService.findAll(q));
        model.addAttribute("search", q);
        model.addAttribute("canManageProducts", canManageProducts(authentication));
        model.addAttribute("canViewCost", canManageProducts(authentication));
        model.addAttribute("pageTitle", "Productos e inventario");
        model.addAttribute("activePage", "productos");
        return "products/list";
    }

    @GetMapping("/nuevo")
    public String newProduct(Model model) {
        model.addAttribute("form", new ProductForm());
        model.addAttribute("formTitle", "Nuevo producto");
        model.addAttribute("formAction", "/productos/nuevo");
        model.addAttribute("activePage", "productos");
        model.addAttribute("pageTitle", "Nuevo producto");
        return "products/form";
    }

    @PostMapping("/nuevo")
    public String create(
            @Valid @ModelAttribute("form") ProductForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Nuevo producto", "/productos/nuevo");
            return "products/form";
        }
        try {
            productService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "El producto se registró con existencia inicial en cero.");
            return "redirect:/productos";
        } catch (DuplicateProductCodeException exception) {
            bindingResult.rejectValue("code", "duplicate", exception.getMessage());
            prepareForm(model, "Nuevo producto", "/productos/nuevo");
            return "products/form";
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar. Revisa la conexión con MySQL y vuelve a intentarlo.");
            prepareForm(model, "Nuevo producto", "/productos/nuevo");
            return "products/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String edit(@PathVariable long id, Model model) {
        try {
            model.addAttribute("form", productService.findForm(id));
        } catch (ProductNotFoundException exception) {
            return "redirect:/productos?notFound";
        }
        model.addAttribute("formTitle", "Editar producto");
        model.addAttribute("formAction", "/productos/" + id + "/editar");
        model.addAttribute("activePage", "productos");
        model.addAttribute("pageTitle", "Editar producto");
        return "products/form";
    }

    @PostMapping("/{id}/editar")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("form") ProductForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareForm(model, "Editar producto", "/productos/" + id + "/editar");
            return "products/form";
        }
        try {
            productService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Los datos del producto se actualizaron.");
            return "redirect:/productos";
        } catch (DuplicateProductCodeException exception) {
            bindingResult.rejectValue("code", "duplicate", exception.getMessage());
            prepareForm(model, "Editar producto", "/productos/" + id + "/editar");
            return "products/form";
        } catch (ProductNotFoundException exception) {
            return "redirect:/productos?notFound";
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar. Revisa la conexión con MySQL y vuelve a intentarlo.");
            prepareForm(model, "Editar producto", "/productos/" + id + "/editar");
            return "products/form";
        }
    }

    @PostMapping("/{id}/desactivar")
    public String deactivate(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deactivate(id);
            redirectAttributes.addFlashAttribute("successMessage", "El producto se desactivó y su historial se conservó.");
        } catch (ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontró el producto o ya estaba desactivado.");
        }
        return "redirect:/productos";
    }

    private static boolean canManageProducts(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AngurUserDetails user)) {
            return false;
        }
        return user.getRole() == UserRole.ADMINISTRADOR || user.getRole() == UserRole.ENCARGADO_ALMACEN;
    }

    private static void prepareForm(Model model, String title, String action) {
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
        model.addAttribute("activePage", "productos");
        model.addAttribute("pageTitle", title);
    }
}
