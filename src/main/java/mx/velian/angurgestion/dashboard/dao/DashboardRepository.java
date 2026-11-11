package mx.velian.angurgestion.dashboard.dao;

import java.math.BigDecimal;
import java.util.List;

import mx.velian.angurgestion.dashboard.model.RecentMovement;

public interface DashboardRepository {
    long countRegisteredSales();

    BigDecimal sumRevenue();

    BigDecimal sumCost();

    List<RecentMovement> findRecentMovements(int limit);
}
