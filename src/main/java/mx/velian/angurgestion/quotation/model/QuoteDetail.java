package mx.velian.angurgestion.quotation.model;

import java.math.BigDecimal;
import java.util.List;

public record QuoteDetail(long id, Long clientId, String clientName, String clientEmail,
                          String clientPhone, long sellerId, String sellerName,
                          QuoteStatus status, String issuedAt, String observations,
                          BigDecimal total, Long saleId, List<QuoteLine> lines) {}
