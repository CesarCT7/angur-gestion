package mx.velian.angurgestion.product.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.velian.angurgestion.product.dao.ProductRepository;
import mx.velian.angurgestion.product.dto.ProductForm;
import mx.velian.angurgestion.product.model.Product;
import mx.velian.angurgestion.product.service.ProductService;
import mx.velian.angurgestion.audit.service.AuditTrail;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final AuditTrail audit;

    public ProductServiceImpl(ProductRepository productRepository, AuditTrail audit) {
        this.productRepository = productRepository;
        this.audit = audit;
    }

    @Override
    public List<Product> findAll(String search) {
        return productRepository.findAll(search);
    }

    @Override
    public List<Product> findArchived(String search) {
        return productRepository.findArchived(search);
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
    public String create(ProductForm form) {
        String code=productRepository.insert(form);
        audit.recordCurrent("PRODUCTO",code,"CREAR","Se creó el producto "+form.getName()+" con código "+code+".");
        return code;
    }

    @Override
    @Transactional
    public void update(long id, ProductForm form) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (!existing.active()) {
            throw new ProductNotFoundException(id);
        }
        if (productRepository.update(id, form) == 0) {
            throw new ProductNotFoundException(id);
        }
        audit.recordCurrent("PRODUCTO",Long.toString(id),"EDITAR","Se actualizaron los datos del producto "+form.getCode()+".");
    }

    @Override
    @Transactional
    public boolean deleteOrArchive(long id) {
        Product product=productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (productRepository.hasHistory(id)) {
            productRepository.archive(id);
            audit.recordCurrent("PRODUCTO",Long.toString(id),"ARCHIVAR","Se archivó el producto "+product.code()+" · "+product.name()+".");
            return true;
        }
        productRepository.deletePermanently(id);
        audit.recordCurrent("PRODUCTO",Long.toString(id),"ELIMINAR","Se eliminó definitivamente el producto "+product.code()+" · "+product.name()+".");
        return false;
    }

    @Override
    @Transactional
    public void restore(long id) {
        Product product=productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (productRepository.restore(id) == 0 && productRepository.findById(id).isEmpty()) {
            throw new ProductNotFoundException(id);
        }
        if(!product.active())audit.recordCurrent("PRODUCTO",Long.toString(id),"RESTAURAR","Se restauró el producto "+product.code()+" · "+product.name()+".");
    }

    @Override
    @Transactional
    public void deletePermanently(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (product.active()) {
            throw new IllegalStateException("Solo se puede eliminar definitivamente un producto archivado.");
        }
        productRepository.deletePermanently(id);
        audit.recordCurrent("PRODUCTO",Long.toString(id),"ELIMINAR","Se eliminó definitivamente el producto archivado "+product.code()+" · "+product.name()+".");
    }
}
