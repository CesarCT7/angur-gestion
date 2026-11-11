package mx.velian.angurgestion.dashboard.model;

public record RecentMovement(
        String dateLabel,
        String productCode,
        String productName,
        String type,
        long quantity,
        String reason,
        String userName) {
}
