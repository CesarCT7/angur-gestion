package mx.velian.angurgestion.quotation.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.quotation.dao.QuoteRepository;
import mx.velian.angurgestion.quotation.dto.QuoteForm;
import mx.velian.angurgestion.quotation.model.QuoteClientOption;
import mx.velian.angurgestion.quotation.model.QuoteDetail;
import mx.velian.angurgestion.quotation.model.QuoteLine;
import mx.velian.angurgestion.quotation.model.QuoteProductOption;
import mx.velian.angurgestion.quotation.model.QuoteStatus;
import mx.velian.angurgestion.quotation.model.QuoteSummary;
import mx.velian.angurgestion.quotation.service.QuoteService;

@Service
public class QuoteServiceImpl implements QuoteService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999.99");
    private final QuoteRepository quotes;

    public QuoteServiceImpl(QuoteRepository quotes) { this.quotes = quotes; }

    @Override
    public List<QuoteSummary> findAll(String search, String rawStatus, UserRole role, long userId) {
        QuoteStatus status = null;
        if (rawStatus != null && !rawStatus.isBlank()) {
            try { status = QuoteStatus.valueOf(rawStatus); }
            catch (IllegalArgumentException ignored) { }
        }
        return quotes.findAll(search, status, role, userId);
    }

    @Override
    public java.util.Optional<QuoteDetail> findById(long id, UserRole role, long userId) {
        return quotes.findById(id, role, userId);
    }

    @Override
    public java.util.Optional<QuoteDetail> findAcceptedForSale(long id, UserRole role, long userId) {
        return quotes.findAcceptedForSale(id, role, userId);
    }

    @Override
    public List<QuoteClientOption> findActiveClients() { return quotes.findActiveClients(); }

    @Override
    public List<QuoteProductOption> findActiveProducts() { return quotes.findActiveProducts(); }

    @Override
    @Transactional
    public long create(QuoteForm form, long sellerId) {
        if (form.getClientId() == null || !quotes.isActiveClient(form.getClientId())) {
            throw new InvalidQuoteException("Selecciona un cliente activo.");
        }
        if (form.getItems() == null || form.getItems().isEmpty()) {
            throw new InvalidQuoteException("Agrega al menos un producto a la cotización.");
        }
        Set<Long> seenProducts = new HashSet<>();
        List<QuoteLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (var item : form.getItems()) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() < 1) {
                throw new InvalidQuoteException("Completa el producto y la cantidad en cada partida.");
            }
            if (!seenProducts.add(item.getProductId())) {
                throw new InvalidQuoteException("Cada producto debe aparecer una sola vez; ajusta su cantidad en esa partida.");
            }
            if (item.getQuantity() > 4_294_967_295L) {
                throw new InvalidQuoteException("La cantidad supera el máximo permitido.");
            }
            QuoteProductOption product = quotes.findActiveProduct(item.getProductId())
                    .orElseThrow(() -> new InvalidQuoteException("Uno de los productos ya no está activo."));
            BigDecimal subtotal = product.salePrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            ensureAmount(subtotal);
            total = total.add(subtotal).setScale(2, RoundingMode.HALF_UP);
            ensureAmount(total);
            lines.add(new QuoteLine(product.id(), product.code(), product.name(), item.getQuantity(),
                    product.salePrice(), subtotal));
        }
        if (form.getObservations() != null && form.getObservations().length() > 1000) {
            throw new InvalidQuoteException("Las observaciones no pueden superar 1000 caracteres.");
        }
        long quoteId = quotes.insertHeader(form.getClientId(), sellerId, form.getObservations(), total);
        for (QuoteLine line : lines) {
            quotes.insertLine(quoteId, line.productId(), line.code(), line.productName(), line.quantity(),
                    line.unitPrice(), line.subtotal());
        }
        return quoteId;
    }

    @Override
    @Transactional
    public void updateStatus(long id, QuoteStatus next, UserRole role, long userId) {
        if (role != UserRole.ADMINISTRADOR && role != UserRole.VENDEDOR) {
            throw new IllegalStateException("Tu perfil solo puede consultar cotizaciones.");
        }
        if (next != QuoteStatus.ACEPTADA && next != QuoteStatus.RECHAZADA) {
            throw new IllegalArgumentException("Solo se puede aceptar o rechazar una cotización pendiente.");
        }
        if (quotes.changePendingStatus(id, next, role, userId) == 0
                && quotes.findById(id, role, userId).isEmpty()) {
            throw new QuoteNotFoundException(id);
        } else if (quotes.findById(id, role, userId).map(QuoteDetail::status).orElse(null) != next) {
            throw new IllegalStateException("La cotización ya no está pendiente y no se puede cambiar su estado.");
        }
    }

    private static void ensureAmount(BigDecimal amount) {
        if (amount.signum() < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidQuoteException("El importe supera el límite admitido.");
        }
    }
}
