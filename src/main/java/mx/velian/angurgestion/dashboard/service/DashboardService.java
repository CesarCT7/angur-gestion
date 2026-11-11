package mx.velian.angurgestion.dashboard.service;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.dashboard.model.DashboardSummary;

public interface DashboardService {
    DashboardSummary summarize(UserRole role);
}
