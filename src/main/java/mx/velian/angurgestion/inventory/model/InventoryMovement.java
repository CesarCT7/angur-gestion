package mx.velian.angurgestion.inventory.model;

public record InventoryMovement(
        long id,
        String date,
        String code,
        String productName,
        MovementType type,
        long quantity,
        long resultingStock,
        String reason,
        String userName) {}
