package mx.velian.angurgestion.auth.service;

import mx.velian.angurgestion.auth.dto.InitialAdminForm;

public interface InitialAdminService {
    boolean isSetupRequired();

    void createInitialAdministrator(InitialAdminForm form);
}
