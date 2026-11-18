package mx.velian.angurgestion.quotation.model;

import java.math.BigDecimal;

public record QuoteLine(Long productId, String code, String productName, long quantity,
                        BigDecimal unitPrice, BigDecimal subtotal) {}
