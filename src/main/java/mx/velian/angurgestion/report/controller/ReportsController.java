package mx.velian.angurgestion.report.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import mx.velian.angurgestion.report.model.*;
import mx.velian.angurgestion.report.service.ReportsService;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ReportsController {
    private static final Logger log = LoggerFactory.getLogger(ReportsController.class);
    private final ReportsService reports;
    public ReportsController(ReportsService reports){this.reports=reports;}

    @GetMapping("/reportes")
    public String index(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="") String producto,
            @RequestParam(defaultValue="") String vendedor,Model model){
        LocalDate end=hasta==null?LocalDate.now():hasta; LocalDate start=desde==null?end.withDayOfMonth(1):desde;
        model.addAttribute("pageTitle","Reportes y ganancias · Análisis y control");model.addAttribute("activePage","analisis");
        model.addAttribute("desde",start);model.addAttribute("hasta",end);model.addAttribute("producto",producto);model.addAttribute("vendedor",vendedor);
        try{var filter=reports.normalize(start,end,producto,vendedor);model.addAttribute("summary",reports.summarize(filter));model.addAttribute("months",reports.byMonth(filter));model.addAttribute("products",reports.byProduct(filter));}
        catch(IllegalArgumentException e){model.addAttribute("formError",e.getMessage());}
        catch(DataAccessException e){log.error("Falló una consulta al generar el módulo de Reportes y ganancias",e);model.addAttribute("formError","No pudimos cargar el reporte. El detalle quedó registrado para revisión.");}
        return "reports/index";
    }

    @GetMapping("/reportes/exportar.csv")
    public ResponseEntity<byte[]> export(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="") String producto,@RequestParam(defaultValue="") String vendedor){
        LocalDate end=hasta==null?LocalDate.now():hasta;LocalDate start=desde==null?end.withDayOfMonth(1):desde;
        var filter=reports.normalize(start,end,producto,vendedor);var summary=reports.summarize(filter);
        String csv=reports.csv(filter,summary,reports.byMonth(filter),reports.byProduct(filter));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=angur-reporte-"+start+"-"+end+".csv")
                .contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
