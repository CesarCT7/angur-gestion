package mx.velian.angurgestion.product.service.impl;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.velian.angurgestion.product.dao.ProductRepository;
import mx.velian.angurgestion.product.dto.ProductForm;
import mx.velian.angurgestion.product.model.Product;
import mx.velian.angurgestion.product.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> findAll(String search) {
        return productRepository.findAll(search);
    }

    @Override
    public ProductForm findForm(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (!product.active()) {
            throw new ProductNotFoundException(id);
        }

        ProductForm form = new ProductForm();
        form.setCode(product.code());
        form.setName(product.name());
        form.setDescription(product.description());
        form.setCost(product.cost());
        form.setSalePrice(product.salePrice());
        return form;
    }

    @Override
    public long countActiveProducts() {
        return productRepository.countActiveProducts();
    }

    @Override
    public long countActiveUnits() {
        return productRepository.countActiveUnits();
    }

    @Override
    @Transactional
    public void create(ProductForm form) {
        if (productRepository.existsByCode(form.getCode())) {
            throw new DuplicateProductCodeException();
        }
        try {
            productRepository.insert(form);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateProductCodeException();
        }
    }

    @Override
    @Transactional
    public void update(long id, ProductForm form) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (!existing.active()) {
            throw new ProductNotFoundException(id);
        }
        if (productRepository.existsByCodeExceptId(form.getCode(), id)) {
            throw new DuplicateProductCodeException();
        }
        try {
            if (productRepository.update(id, form) == 0) {
                throw new ProductNotFoundException(id);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateProductCodeException();
        }
    }

    @Override
    @Transactional
    public void deactivate(long id) {
        if (productRepository.deactivate(id) == 0 && productRepository.findById(id).isEmpty()) {
            throw new ProductNotFoundException(id);
        }
    }
}
