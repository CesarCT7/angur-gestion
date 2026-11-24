package mx.velian.angurgestion.history.service;

import java.time.LocalDate;
import java.util.List;
import mx.velian.angurgestion.history.dao.HistoryRepository;
import mx.velian.angurgestion.history.model.HistoryEntry;
import mx.velian.angurgestion.history.model.HistoryFilter;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {
    private final HistoryRepository history;
    public HistoryService(HistoryRepository history){this.history=history;}
    public List<HistoryEntry> find(LocalDate from, LocalDate to, String type, String status,
            String product, String actor, String search){
        LocalDate end=to==null?LocalDate.now():to;
        LocalDate start=from==null?end.minusDays(29):from;
        if(start.isAfter(end)) throw new IllegalArgumentException("La fecha inicial debe ser anterior o igual a la fecha final.");
        if(end.isAfter(LocalDate.now().plusDays(1))) throw new IllegalArgumentException("La fecha final no puede estar en el futuro.");
        if(start.isBefore(end.minusYears(5))) throw new IllegalArgumentException("El historial permite consultar hasta cinco años por búsqueda.");
        return history.find(new HistoryFilter(start,end,type,status,product,actor,search));
    }
}
