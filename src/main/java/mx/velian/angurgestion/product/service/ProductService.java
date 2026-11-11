package mx.velian.angurgestion.product.service;

import java.util.List;

import mx.velian.angurgestion.product.dto.ProductForm;
import mx.velian.angurgestion.product.model.Product;

public interface ProductService {
    List<Product> findAll(String search);

    ProductForm findForm(long id);

    long countActiveProducts();

    long countActiveUnits();

    void create(ProductForm form);

    void update(long id, ProductForm form);

    void deactivate(long id);
}
