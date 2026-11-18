package mx.velian.angurgestion.sale.model;

import java.math.BigDecimal;

public record SaleOperation(long id, long sellerId, Long clientId, Long quoteId, BigDecimal total) {}
