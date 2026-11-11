package mx.velian.angurgestion.dashboard.model;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummary(
        long activeProducts,
        long unitsInStock,
        long registeredSales,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal grossProfit,
        List<RecentMovement> recentMovements) {
}
