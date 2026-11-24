package mx.velian.angurgestion.report.service;

import java.time.LocalDate;
import java.util.List;
import mx.velian.angurgestion.report.dao.ReportsRepository;
import mx.velian.angurgestion.report.model.*;
import org.springframework.stereotype.Service;

@Service
public class ReportsService {
    private final ReportsRepository reports;
    public ReportsService(ReportsRepository reports){this.reports=reports;}

    public ReportFilter normalize(LocalDate from, LocalDate to, String product, String seller){
        LocalDate end=to==null?LocalDate.now():to;
        LocalDate start=from==null?end.withDayOfMonth(1):from;
        if(start.isAfter(end))throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la fecha final.");
        if(end.isAfter(LocalDate.now().plusDays(1)))throw new IllegalArgumentException("La fecha final no puede estar en el futuro.");
        if(start.isBefore(end.minusYears(5)))throw new IllegalArgumentException("El reporte permite consultar hasta cinco años por búsqueda.");
        return new ReportFilter(start,end,clean(product),clean(seller));
    }
    public ReportSummary summarize(ReportFilter f){return reports.summarize(f);}
    public List<MonthlyReportRow> byMonth(ReportFilter f){return reports.byMonth(f);}
    public List<ProductReportRow> byProduct(ReportFilter f){return reports.byProduct(f);}
    private String clean(String v){return v==null||v.isBlank()?null:v.trim();}

    public String csv(ReportFilter f,ReportSummary summary,List<MonthlyReportRow> months,List<ProductReportRow> products){
        StringBuilder out=new StringBuilder("\uFEFFTipo,Periodo/Código,Producto/Ventas,Unidades,Ingresos,Costos,Utilidad bruta\r\n");
        out.append(row("RESUMEN",f.from()+" a "+f.to(),Long.toString(summary.salesCount()),"",money(summary.revenue()),money(summary.cost()),money(summary.grossProfit())));
        for(var m:months)out.append(row("MES",m.month(),Long.toString(m.salesCount()),"",money(m.revenue()),money(m.cost()),money(m.grossProfit())));
        for(var p:products)out.append(row("PRODUCTO",p.code(),p.product(),Long.toString(p.units()),money(p.revenue()),money(p.cost()),money(p.grossProfit())));
        return out.toString();
    }
    private String money(java.math.BigDecimal v){return v.toPlainString();}
    private String row(String... values){
        StringBuilder s=new StringBuilder();
        for(int i=0;i<values.length;i++){if(i>0)s.append(',');String value=values[i]==null?"":values[i].replaceAll("[\\p{Cntrl}]"," ");if(value.matches("^[=+@-].*"))value="'"+value;s.append('"').append(value.replace("\"","\"\"")).append('"');}
        return s.append("\r\n").toString();
    }
}
