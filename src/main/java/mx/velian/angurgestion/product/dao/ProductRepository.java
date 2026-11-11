package mx.velian.angurgestion.product.dao;

import java.util.List;
import java.util.Optional;

import mx.velian.angurgestion.product.dto.ProductForm;
import mx.velian.angurgestion.product.model.Product;

public interface ProductRepository {
    List<Product> findAll(String search);

    Optional<Product> findById(long id);

    boolean existsByCode(String code);

    boolean existsByCodeExceptId(String code, long id);

    long countActiveProducts();

    long countActiveUnits();

    void insert(ProductForm form);

    int update(long id, ProductForm form);

    int deactivate(long id);
}
