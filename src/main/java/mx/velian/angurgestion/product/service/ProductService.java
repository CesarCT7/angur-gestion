package mx.velian.angurgestion.product.service;

import java.util.List;

import mx.velian.angurgestion.product.dto.ProductForm;
import mx.velian.angurgestion.product.model.Product;

public interface ProductService {
    List<Product> findAll(String search);

    List<Product> findArchived(String search);

    ProductForm findForm(long id);

    long countActiveProducts();

    long countActiveUnits();

    String create(ProductForm form);

    void update(long id, ProductForm form);

    boolean deleteOrArchive(long id);

    void restore(long id);

    void deletePermanently(long id);
}
