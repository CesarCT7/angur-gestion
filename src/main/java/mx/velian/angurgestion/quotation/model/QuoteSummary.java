package mx.velian.angurgestion.quotation.model;

import java.math.BigDecimal;

public record QuoteSummary(long id, String issuedAt, String clientName, String sellerName,
                           QuoteStatus status, BigDecimal total, Long saleId) {}
