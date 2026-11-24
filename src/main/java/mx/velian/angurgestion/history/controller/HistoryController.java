package mx.velian.angurgestion.history.controller;

import java.time.LocalDate;
import mx.velian.angurgestion.history.service.HistoryService;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HistoryController {
    private final HistoryService history;
    public HistoryController(HistoryService history){this.history=history;}

    @GetMapping("/analisis")
    public ModelAndView analysisHome(){return new ModelAndView("redirect:/historial");}

    @GetMapping("/historial")
    public String index(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="") String tipo,@RequestParam(defaultValue="") String estado,
            @RequestParam(defaultValue="") String producto,
            @RequestParam(defaultValue="") String usuario,@RequestParam(defaultValue="") String q,Model model){
        LocalDate end=hasta==null?LocalDate.now():hasta; LocalDate start=desde==null?end.minusDays(29):desde;
        model.addAttribute("pageTitle","Historial y auditoría · Análisis y control");model.addAttribute("activePage","analisis");
        model.addAttribute("desde",start);model.addAttribute("hasta",end);model.addAttribute("tipo",tipo);model.addAttribute("estado",estado);
        model.addAttribute("producto",producto);model.addAttribute("usuario",usuario);model.addAttribute("q",q);
        try{model.addAttribute("entries",history.find(start,end,tipo,estado,producto,usuario,q));}
        catch(IllegalArgumentException e){model.addAttribute("formError",e.getMessage());model.addAttribute("entries",java.util.List.of());}
        catch(DataAccessException e){model.addAttribute("formError","No pudimos cargar el historial. Revisa la conexión con MySQL.");model.addAttribute("entries",java.util.List.of());}
        return "history/list";
    }
}
