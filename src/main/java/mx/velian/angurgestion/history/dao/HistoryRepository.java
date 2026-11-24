package mx.velian.angurgestion.history.dao;

import java.util.List;
import mx.velian.angurgestion.history.model.HistoryEntry;
import mx.velian.angurgestion.history.model.HistoryFilter;

public interface HistoryRepository {
    List<HistoryEntry> find(HistoryFilter filter);
}
