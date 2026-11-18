package mx.velian.angurgestion.inventory.service;

import java.util.List;

import mx.velian.angurgestion.inventory.dto.InventoryForm;
import mx.velian.angurgestion.inventory.model.InventoryItem;
import mx.velian.angurgestion.inventory.model.InventoryMovement;
import mx.velian.angurgestion.inventory.model.InventoryResult;

public interface InventoryService {
    List<InventoryItem> findItems(String search);
    List<InventoryMovement> findRecentMovements(String search, int limit);
    InventoryResult record(InventoryForm form, long userId);
}
