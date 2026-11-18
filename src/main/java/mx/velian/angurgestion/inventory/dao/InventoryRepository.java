package mx.velian.angurgestion.inventory.dao;

import java.util.List;
import java.util.Optional;

import mx.velian.angurgestion.inventory.model.InventoryItem;
import mx.velian.angurgestion.inventory.model.InventoryMovement;
import mx.velian.angurgestion.inventory.model.MovementType;

public interface InventoryRepository {
    List<InventoryItem> findItems(String search);
    List<InventoryMovement> findRecentMovements(String search, int limit);
    Optional<InventoryMovement> findByOperation(String key, long productId, MovementType type);
    Optional<InventoryItem> lockActiveItem(long productId);
    int updateStock(long productId, long newStock);
    void insertMovement(long productId, String code, String name, long userId,
            MovementType type, long quantity, long resultingStock, String reason, String key);
}
