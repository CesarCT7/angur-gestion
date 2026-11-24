package mx.velian.angurgestion.report.dao;

import java.util.List;
import mx.velian.angurgestion.report.model.MonthlyReportRow;
import mx.velian.angurgestion.report.model.ProductReportRow;
import mx.velian.angurgestion.report.model.ReportFilter;
import mx.velian.angurgestion.report.model.ReportSummary;

public interface ReportsRepository {
    ReportSummary summarize(ReportFilter filter);
    List<MonthlyReportRow> byMonth(ReportFilter filter);
    List<ProductReportRow> byProduct(ReportFilter filter);
}
