package mx.velian.angurgestion.dashboard.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.dashboard.dao.DashboardRepository;
import mx.velian.angurgestion.dashboard.model.DashboardSummary;
import mx.velian.angurgestion.dashboard.model.RecentMovement;
import mx.velian.angurgestion.dashboard.service.DashboardService;
import mx.velian.angurgestion.product.dao.ProductRepository;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final ProductRepository productRepository;
    private final DashboardRepository dashboardRepository;

    public DashboardServiceImpl(ProductRepository productRepository, DashboardRepository dashboardRepository) {
        this.productRepository = productRepository;
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public DashboardSummary summarize(UserRole role) {
        long productCount = productRepository.countActiveProducts();
        long units = productRepository.countActiveUnits();

        boolean canSeeFinancials = role == UserRole.ADMINISTRADOR || role == UserRole.SUPERVISOR_VENTAS;
        boolean canSeeMovements = role == UserRole.ADMINISTRADOR || role == UserRole.ENCARGADO_ALMACEN;

        long sales = canSeeFinancials ? dashboardRepository.countRegisteredSales() : 0;
        BigDecimal revenue = canSeeFinancials ? dashboardRepository.sumRevenue() : BigDecimal.ZERO;
        BigDecimal cost = canSeeFinancials ? dashboardRepository.sumCost() : BigDecimal.ZERO;
        BigDecimal profit = canSeeFinancials ? revenue.subtract(cost) : BigDecimal.ZERO;
        List<RecentMovement> movements = canSeeMovements
                ? dashboardRepository.findRecentMovements(6)
                : List.of();

        return new DashboardSummary(productCount, units, sales, revenue, cost, profit, movements);
    }
}
