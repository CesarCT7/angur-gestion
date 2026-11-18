package mx.velian.angurgestion.sale.service;

import java.util.List;
import java.util.Optional;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.quotation.model.QuoteClientOption;
import mx.velian.angurgestion.quotation.model.QuoteProductOption;
import mx.velian.angurgestion.sale.dto.SaleForm;
import mx.velian.angurgestion.sale.model.SaleDetail;
import mx.velian.angurgestion.sale.model.SaleResult;
import mx.velian.angurgestion.sale.model.SaleSummary;

public interface SaleService {
    List<SaleSummary> findAll(String search, String status, UserRole role, long userId);
    Optional<SaleDetail> findById(long id, UserRole role, long userId);
    List<QuoteClientOption> findActiveClients();
    List<QuoteProductOption> findActiveProducts();
    SaleResult create(SaleForm form, long sellerId, UserRole role);
}
