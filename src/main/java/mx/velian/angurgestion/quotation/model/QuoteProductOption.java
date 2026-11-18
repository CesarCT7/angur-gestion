package mx.velian.angurgestion.quotation.model;

import java.math.BigDecimal;

public record QuoteProductOption(long id, String code, String name, BigDecimal salePrice) {}
