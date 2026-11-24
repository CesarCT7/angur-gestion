package mx.velian.angurgestion.report.model;

import java.math.BigDecimal;

public record ReportSummary(long salesCount, BigDecimal revenue, BigDecimal cost, BigDecimal grossProfit) {}
