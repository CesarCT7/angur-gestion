package mx.velian.angurgestion.sale.model;

import java.math.BigDecimal;
import java.util.List;

public record SaleDetail(long id, Long clientId, String clientName, long sellerId,
                         String sellerName, Long quoteId, String date, SaleStatus status,
                         BigDecimal total, List<SaleLine> lines) {}
