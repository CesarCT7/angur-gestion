package mx.velian.angurgestion.auth.service.impl;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.dto.InitialAdminForm;
import mx.velian.angurgestion.auth.service.InitialAdminService;

@Service
public class InitialAdminServiceImpl implements InitialAdminService {
    private static final String SETUP_LOCK = "angur_gestion_initial_admin";
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminServiceImpl(
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean isSetupRequired() {
        return userRepository.countUsers() == 0;
    }

    @Override
    @Transactional
    public void createInitialAdministrator(InitialAdminForm form) {
        Integer lockAcquired = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 10)", Integer.class, SETUP_LOCK);
        if (lockAcquired == null || lockAcquired != 1) {
            throw new IllegalStateException("No se pudo iniciar la configuración. Inténtalo de nuevo.");
        }

        try {
            if (userRepository.countUsers() > 0) {
                throw new IllegalStateException("La configuración inicial ya se completó.");
            }
            if (userRepository.existsByUsernameOrEmail(form.getUsername(), form.getEmail())) {
                throw new DataIntegrityViolationException("El usuario o correo ya está registrado.");
            }
            if (!form.getPassword().equals(form.getPasswordConfirmation())) {
                throw new IllegalArgumentException("Las contraseñas no coinciden.");
            }

            String passwordHash = passwordEncoder.encode(form.getPassword());
            userRepository.createInitialAdministrator(
                    form.getUsername(), form.getEmail(), form.getName(), passwordHash);
        } finally {
            jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, SETUP_LOCK);
        }
    }
}
