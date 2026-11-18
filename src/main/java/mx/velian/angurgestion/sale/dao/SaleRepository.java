package mx.velian.angurgestion.sale.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.sale.model.SaleDetail;
import mx.velian.angurgestion.sale.model.SaleOperation;
import mx.velian.angurgestion.sale.model.SaleStatus;
import mx.velian.angurgestion.sale.model.SaleSummary;
import mx.velian.angurgestion.sale.model.SaleLine;

public interface SaleRepository {
    List<SaleSummary> findAll(String search, SaleStatus status, UserRole role, long userId);
    Optional<SaleDetail> findById(long id, UserRole role, long userId);
    Optional<SaleOperation> findByOperationKey(String key);
    Optional<SaleOperation> findCommittedByOperationKey(String key);
    Optional<SaleOperation> findCommittedByQuoteId(long quoteId);
    List<SaleLine> findQuoteLines(long quoteId);
    long insertHeader(Long clientId, Long quoteId, long sellerId, String key, BigDecimal total);
    void insertLine(long saleId, long productId, String code, String name, long quantity,
                    BigDecimal unitPrice, BigDecimal unitCost, BigDecimal subtotal);
    void insertMovement(long saleId, long productId, String code, String name, long userId,
                        long quantity, long resultingStock, String key);
}
