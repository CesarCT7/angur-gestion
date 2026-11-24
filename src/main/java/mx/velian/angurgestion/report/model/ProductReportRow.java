package mx.velian.angurgestion.report.model;

import java.math.BigDecimal;

public record ProductReportRow(String code, String product, long units, BigDecimal revenue, BigDecimal cost, BigDecimal grossProfit) {}
