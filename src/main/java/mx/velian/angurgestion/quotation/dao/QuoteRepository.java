package mx.velian.angurgestion.quotation.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.quotation.model.QuoteClientOption;
import mx.velian.angurgestion.quotation.model.QuoteDetail;
import mx.velian.angurgestion.quotation.model.QuoteProductOption;
import mx.velian.angurgestion.quotation.model.QuoteStatus;
import mx.velian.angurgestion.quotation.model.QuoteSummary;

public interface QuoteRepository {
    List<QuoteSummary> findAll(String search, QuoteStatus status, UserRole role, long userId);
    Optional<QuoteDetail> findById(long id, UserRole role, long userId);
    Optional<QuoteDetail> findAcceptedForSale(long id, UserRole role, long userId);
    List<QuoteClientOption> findActiveClients();
    List<QuoteProductOption> findActiveProducts();
    Optional<QuoteProductOption> findActiveProduct(long id);
    boolean isActiveClient(long id);
    long insertHeader(long clientId, long sellerId, String observations, BigDecimal total);
    void insertLine(long quoteId, long productId, String code, String name, long quantity,
                    BigDecimal unitPrice, BigDecimal subtotal);
    int changePendingStatus(long id, QuoteStatus next, UserRole role, long userId);
}
