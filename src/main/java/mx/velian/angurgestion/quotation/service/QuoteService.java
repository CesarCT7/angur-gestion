package mx.velian.angurgestion.quotation.service;

import java.util.List;
import java.util.Optional;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.quotation.dto.QuoteForm;
import mx.velian.angurgestion.quotation.model.QuoteDetail;
import mx.velian.angurgestion.quotation.model.QuoteProductOption;
import mx.velian.angurgestion.quotation.model.QuoteStatus;
import mx.velian.angurgestion.quotation.model.QuoteSummary;

public interface QuoteService {
    List<QuoteSummary> findAll(String search, String status, UserRole role, long userId);
    Optional<QuoteDetail> findById(long id, UserRole role, long userId);
    Optional<QuoteDetail> findAcceptedForSale(long id, UserRole role, long userId);
    List<QuoteProductOption> findActiveProducts();
    long create(QuoteForm form, long sellerId);
    void updateStatus(long id, QuoteStatus next, UserRole role, long userId);
}
