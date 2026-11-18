package mx.velian.angurgestion.sale.model;

import java.math.BigDecimal;

public record SaleLine(Long productId, String code, String productName, long quantity,
                       BigDecimal unitPrice, BigDecimal unitCost, BigDecimal subtotal) {}
