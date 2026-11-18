package mx.velian.angurgestion.inventory.service.impl;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import mx.velian.angurgestion.inventory.dao.InventoryRepository;
import mx.velian.angurgestion.inventory.dto.InventoryForm;
import mx.velian.angurgestion.inventory.model.InventoryItem;
import mx.velian.angurgestion.inventory.model.InventoryMovement;
import mx.velian.angurgestion.inventory.model.InventoryResult;
import mx.velian.angurgestion.inventory.model.MovementType;
import mx.velian.angurgestion.inventory.service.InventoryService;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventory;

    public InventoryServiceImpl(InventoryRepository inventory) {
        this.inventory = inventory;
    }

    @Override
    public List<InventoryItem> findItems(String search) {
        return inventory.findItems(search);
    }

    @Override
    public List<InventoryMovement> findRecentMovements(String search, int limit) {
        return inventory.findRecentMovements(search, Math.max(1, Math.min(limit, 200)));
    }

    @Override
    @Transactional
    public InventoryResult record(InventoryForm form, long userId) {
        long productId = form.getProductId();
        String key = form.getOperationKey().trim();
        MovementType type = form.getType();

        InventoryMovement previous = inventory.findByOperation(key, productId, type).orElse(null);
        if (previous != null) return new InventoryResult(previous.resultingStock(), true);

        InventoryItem item = inventory.lockActiveItem(productId)
                .orElseThrow(() -> new InventoryProductNotFoundException(productId));
        long quantity = form.getQuantity();
        if (type == MovementType.SALIDA && quantity > item.stock()) {
            throw new InsufficientStockException(item.stock());
        }
        long resulting = type == MovementType.ENTRADA ? item.stock() + quantity : item.stock() - quantity;
        if (resulting > 4_294_967_295L) {
            throw new IllegalArgumentException("La existencia resultante supera el máximo admitido.");
        }
        if (inventory.updateStock(productId, resulting) == 0) {
            throw new InventoryProductNotFoundException(productId);
        }
        try {
            inventory.insertMovement(productId, item.code(), item.name(), userId, type,
                    quantity, resulting, form.getReason(), key);
        } catch (DuplicateKeyException exception) {
            InventoryMovement concurrent = inventory.findByOperation(key, productId, type).orElse(null);
            if (concurrent == null) throw exception;
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new InventoryResult(concurrent.resultingStock(), true);
        }
        return new InventoryResult(resulting, false);
    }
}
