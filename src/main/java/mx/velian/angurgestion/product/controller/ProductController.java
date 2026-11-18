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
        model.addAttribute("archived", false);
        model.addAttribute("canManageProducts", canManageProducts(authentication));
        model.addAttribute("canViewCost", canManageProducts(authentication));
        model.addAttribute("pageTitle", "Productos e inventario");
        model.addAttribute("activePage", "productos");
        return "products/list";
    }

    @GetMapping("/archivados")
    public String archived(@RequestParam(defaultValue = "") String q, Authentication authentication, Model model) {
        model.addAttribute("products", productService.findArchived(q));
        model.addAttribute("search", q);
        model.addAttribute("archived", true);
        model.addAttribute("canManageProducts", canManageProducts(authentication));
        model.addAttribute("canViewCost", canManageProducts(authentication));
        model.addAttribute("pageTitle", "Productos archivados");
        model.addAttribute("activePage", "productos");
        return "products/list";
    }

    @GetMapping("/nuevo")
    public String newProduct(Model model) {
        model.addAttribute("form", new ProductForm());
        model.addAttribute("productCode", null);
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
            String code = productService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Producto registrado con el código " + code + " y existencia inicial en cero.");
            return "redirect:/productos";
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar. Revisa la conexión con MySQL y vuelve a intentarlo.");
            prepareForm(model, "Nuevo producto", "/productos/nuevo");
            return "products/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String edit(@PathVariable long id, Model model) {
        try {
            ProductForm form = productService.findForm(id);
            model.addAttribute("form", form);
            model.addAttribute("productCode", form.getCode());
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
            model.addAttribute("productCode", productService.findForm(id).getCode());
            return "products/form";
        }
        try {
            productService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Los datos del producto se actualizaron.");
            return "redirect:/productos";
        } catch (ProductNotFoundException exception) {
            return "redirect:/productos?notFound";
        } catch (DataAccessException exception) {
            model.addAttribute("databaseError", "No se pudo guardar. Revisa la conexión con MySQL y vuelve a intentarlo.");
            prepareForm(model, "Editar producto", "/productos/" + id + "/editar");
            model.addAttribute("productCode", productService.findForm(id).getCode());
            return "products/form";
        }
    }

    @PostMapping("/{id}/desactivar")
    public String deactivate(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            boolean archived = productService.deleteOrArchive(id);
            redirectAttributes.addFlashAttribute("successMessage", archived
                    ? "El producto se archivó porque conserva historial o existencia. Puedes restaurarlo o eliminarlo definitivamente desde Archivados."
                    : "El producto se eliminó definitivamente porque no tiene historial ni existencia.");
        } catch (ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontró el producto o ya estaba desactivado.");
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No fue posible eliminar el producto. Revisa la conexión con MySQL e inténtalo de nuevo.");
        }
        return "redirect:/productos";
    }

    @PostMapping("/{id}/restaurar")
    public String restore(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            productService.restore(id);
            redirectAttributes.addFlashAttribute("successMessage", "El producto se restauró al catálogo.");
        } catch (ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontró el producto archivado.");
        }
        return "redirect:/productos/archivados";
    }

    @PostMapping("/{id}/eliminar-definitivamente")
    public String deletePermanently(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deletePermanently(id);
            redirectAttributes.addFlashAttribute("successMessage", "El producto se eliminó definitivamente. Las ventas, cotizaciones y movimientos históricos se conservaron.");
        } catch (ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontró el producto archivado.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "No fue posible eliminar el producto. Revisa la conexión con MySQL e inténtalo de nuevo.");
        }
        return "redirect:/productos/archivados";
    }

    private static boolean canManageProducts(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AngurUserDetails user)) {
            return false;
        }
        return user.getRole() == UserRole.ADMINISTRADOR || user.getRole() == UserRole.ENCARGADO_ALMACEN;
    }

    private static void prepareForm(Model model, String title, String action) {
        if (!model.containsAttribute("productCode")) model.addAttribute("productCode", null);
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", action);
        model.addAttribute("activePage", "productos");
        model.addAttribute("pageTitle", title);
    }
}
