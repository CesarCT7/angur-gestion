package mx.velian.angurgestion.sale.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.inventory.dao.InventoryRepository;
import mx.velian.angurgestion.inventory.model.InventoryItem;
import mx.velian.angurgestion.quotation.dao.QuoteRepository;
import mx.velian.angurgestion.quotation.model.QuoteDetail;
import mx.velian.angurgestion.quotation.model.QuoteProductOption;
import mx.velian.angurgestion.product.dao.ProductRepository;
import mx.velian.angurgestion.product.model.Product;
import mx.velian.angurgestion.sale.dao.SaleRepository;
import mx.velian.angurgestion.sale.dto.SaleForm;
import mx.velian.angurgestion.sale.dto.SaleLineForm;
import mx.velian.angurgestion.sale.model.SaleDetail;
import mx.velian.angurgestion.sale.model.SaleLine;
import mx.velian.angurgestion.sale.model.SaleOperation;
import mx.velian.angurgestion.sale.model.SaleResult;
import mx.velian.angurgestion.sale.model.SaleStatus;
import mx.velian.angurgestion.sale.model.SaleSummary;
import mx.velian.angurgestion.sale.service.SaleService;
import mx.velian.angurgestion.audit.service.AuditTrail;

@Service
public class SaleServiceImpl implements SaleService {
    private final SaleRepository sales;
    private final QuoteRepository quotes;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final AuditTrail audit;

    public SaleServiceImpl(SaleRepository sales, QuoteRepository quotes, ProductRepository products,
                           InventoryRepository inventory, AuditTrail audit) {
        this.sales = sales;
        this.quotes = quotes;
        this.products = products;
        this.inventory = inventory;
        this.audit=audit;
    }

    @Override
    public List<SaleSummary> findAll(String search, String status, UserRole role, long userId) {
        SaleStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try { parsed = SaleStatus.valueOf(status); } catch (IllegalArgumentException ignored) { parsed = null; }
        }
        return sales.findAll(search, parsed, role, userId);
    }

    @Override public Optional<SaleDetail> findById(long id, UserRole role, long userId) {
        return sales.findById(id, role, userId);
    }
    @Override public List<QuoteProductOption> findActiveProducts() { return quotes.findActiveProducts(); }

    @Override
    @Transactional
    public SaleResult create(SaleForm form, long sellerId, UserRole role) {
        if (role != UserRole.ADMINISTRADOR && role != UserRole.VENDEDOR) {
            throw new InvalidSaleException("Tu perfil no puede registrar ventas.");
        }
        if (form.getOperationKey() == null || !form.getOperationKey().matches("[0-9a-fA-F-]{36}")) {
            throw new InvalidSaleException("Actualiza el formulario para generar una nueva clave de operación.");
        }
        Optional<SaleOperation> prior = sales.findByOperationKey(form.getOperationKey());
        if (prior.isPresent()) {
            SaleOperation operation = prior.get();
            if (role == UserRole.VENDEDOR && operation.sellerId() != sellerId) {
                throw new InvalidSaleException("La clave de operación no está disponible.");
            }
            return new SaleResult(operation.id(), operation.total(), true);
        }

        Long clientId;
        Long quoteId = form.getQuoteId();
        List<LineDraft> drafts;
        if (quoteId != null) {
            QuoteDetail quote = quotes.findAcceptedForSale(quoteId, role, sellerId)
                    .orElseThrow(() -> new InvalidSaleException("La cotización no está aceptada, no existe o no tienes acceso."));
            if (sales.findCommittedByQuoteId(quoteId).isPresent()) throw new QuoteAlreadySoldException(quoteId);
            clientId = quote.clientId();
            drafts = new ArrayList<>();
            for (SaleLine line : sales.findQuoteLines(quoteId)) {
                if (line.productId() == null) throw new InvalidSaleException("Un producto de la cotización ya no está disponible.");
                Product product = products.findById(line.productId()).filter(Product::active)
                        .orElseThrow(() -> new InvalidSaleException("Un producto de la cotización ya no está activo."));
                drafts.add(new LineDraft(product, line.quantity(), line.unitPrice()));
            }
        } else {
            clientId = null;
            if (form.getItems() == null || form.getItems().isEmpty()) throw new InvalidSaleException("Agrega al menos un producto.");
            drafts = new ArrayList<>();
            Set<Long> unique = new HashSet<>();
            for (SaleLineForm line : form.getItems()) {
                if (line == null || line.getProductId() == null || line.getQuantity() == null || line.getQuantity() < 1) {
                    throw new InvalidSaleException("Selecciona productos y cantidades válidas.");
                }
                if (!unique.add(line.getProductId())) throw new InvalidSaleException("Cada producto debe aparecer una sola vez.");
                Product product = products.findById(line.getProductId()).filter(Product::active)
                        .orElseThrow(() -> new InvalidSaleException("Uno de los productos ya no está activo."));
                drafts.add(new LineDraft(product, line.getQuantity(), product.salePrice()));
            }
        }
        if (drafts.isEmpty()) throw new InvalidSaleException("La venta debe incluir productos.");

        List<LineDraft> ordered = drafts.stream().sorted((a, b) -> Long.compare(a.product().id(), b.product().id())).toList();
        BigDecimal total = BigDecimal.ZERO;
        for (LineDraft line : ordered) total = total.add(line.price().multiply(BigDecimal.valueOf(line.quantity())));
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (total.signum() <= 0 || total.precision() > 13) throw new InvalidSaleException("El total de la venta excede el límite permitido.");

        long saleId;
        try {
            saleId = sales.insertHeader(clientId, quoteId, sellerId, form.getOperationKey(), total);
        } catch (DuplicateKeyException collision) {
            Optional<SaleOperation> sameKey = sales.findCommittedByOperationKey(form.getOperationKey());
            if (sameKey.isPresent()) return new SaleResult(sameKey.get().id(), sameKey.get().total(), true);
            if (quoteId != null && sales.findCommittedByQuoteId(quoteId).isPresent()) {
                throw new QuoteAlreadySoldException(quoteId);
            }
            throw collision;
        }

        List<InventoryItem> locked = new ArrayList<>();
        for (LineDraft draft : ordered) {
            InventoryItem item = inventory.lockActiveItem(draft.product().id())
                    .orElseThrow(() -> new InvalidSaleException("Un producto dejó de estar disponible en inventario."));
            if (item.stock() < draft.quantity()) {
                throw new InsufficientSaleStockException(item.name(), item.stock());
            }
            locked.add(item);
        }
        for (int i = 0; i < ordered.size(); i++) {
            LineDraft draft = ordered.get(i);
            InventoryItem item = locked.get(i);
            long resultingStock = item.stock() - draft.quantity();
            BigDecimal subtotal = draft.price().multiply(BigDecimal.valueOf(draft.quantity())).setScale(2, RoundingMode.HALF_UP);
            sales.insertLine(saleId, item.id(), item.code(), item.name(), draft.quantity(), draft.price(), draft.product().cost(), subtotal);
            inventory.updateStock(item.id(), resultingStock);
            sales.insertMovement(saleId, item.id(), item.code(), item.name(), sellerId, draft.quantity(), resultingStock,
                    form.getOperationKey() + ":" + item.id());
        }
        audit.record(sellerId,"VENTA",Long.toString(saleId),"REGISTRAR","Se registró la venta #"+saleId+" con "+ordered.size()+" productos por un total de $"+total+".");
        return new SaleResult(saleId, total, false);
    }

    private record LineDraft(Product product, long quantity, BigDecimal price) {}
}
