package mx.velian.angurgestion.sale.model;

import java.math.BigDecimal;

public record SaleResult(long id, BigDecimal total, boolean alreadyProcessed) {}
