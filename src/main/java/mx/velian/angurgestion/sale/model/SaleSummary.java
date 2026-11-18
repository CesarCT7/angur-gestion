package mx.velian.angurgestion.sale.model;

import java.math.BigDecimal;

public record SaleSummary(long id, String date, String clientName, String sellerName,
                          Long quoteId, SaleStatus status, BigDecimal total) {}
