package mx.velian.angurgestion.report.model;

import java.math.BigDecimal;

public record MonthlyReportRow(String month, long salesCount, BigDecimal revenue, BigDecimal cost, BigDecimal grossProfit) {}
