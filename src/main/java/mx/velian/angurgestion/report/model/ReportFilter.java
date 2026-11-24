package mx.velian.angurgestion.report.model;

import java.time.LocalDate;

public record ReportFilter(LocalDate from, LocalDate to, String product, String seller) {}
