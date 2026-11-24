package mx.velian.angurgestion.auth.service.impl;

import jakarta.validation.Validator;

import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.dto.InitialAdminForm;
import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.auth.service.InitialAdminService;
import mx.velian.angurgestion.audit.service.AuditTrail;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitialAdminServiceImpl implements InitialAdminService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final AuditTrail audit;

    public InitialAdminServiceImpl(
            UserRepository users, PasswordEncoder encoder, Validator validator, AuditTrail audit) {
        this.users = users;
        this.encoder = encoder;
        this.validator = validator;
        this.audit=audit;
    }

    public boolean isSetupRequired() {
        return users.countUsers() == 0;
    }

    @Transactional
    public void createInitialAdministrator(InitialAdminForm form) {
        if (!validator.validate(form).isEmpty()
                || !form.getPassword().equals(form.getPasswordConfirmation()))
            throw new IllegalArgumentException("Revisa los datos y la contraseña.");
        users.lockSetup();
        if (users.countUsers() > 0)
            throw new IllegalStateException("La configuración inicial ya se completó.");
        if (users.existsByUsernameOrEmail(form.getUsername(), form.getEmail()))
            throw new DataIntegrityViolationException("Cuenta duplicada");
        long administratorId = users.create(
                form.getUsername(),
                form.getEmail(),
                form.getName(),
                encoder.encode(form.getPassword()),
                UserRole.ADMINISTRADOR,
                true);
        users.completeSetup(administratorId);
        audit.record(administratorId,"CUENTA",Long.toString(administratorId),"CONFIGURACION_INICIAL","Se creó el administrador principal de la instalación.");
    }
}
