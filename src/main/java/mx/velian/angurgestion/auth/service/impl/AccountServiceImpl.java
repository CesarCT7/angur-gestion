package mx.velian.angurgestion.auth.service.impl;

import jakarta.validation.Validator;

import mx.velian.angurgestion.auth.config.AuthProperties;
import mx.velian.angurgestion.auth.dao.*;
import mx.velian.angurgestion.auth.dto.*;
import mx.velian.angurgestion.auth.model.*;
import mx.velian.angurgestion.auth.service.AccountService;
import mx.velian.angurgestion.audit.service.AuditTrail;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class AccountServiceImpl implements AccountService {
    public static final String VERIFY = "VERIFICAR_CORREO", RESET = "RESTABLECER_CLAVE";
    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final AuthMailService mail;
    private final AuthProperties properties;
    private final Clock clock;
    private final Validator validator;
    private final AuditTrail audit;
    private final SecureRandom random = new SecureRandom();

    public AccountServiceImpl(
            UserRepository users,
            AuthTokenRepository tokens,
            PasswordEncoder encoder,
            AuthMailService mail,
            AuthProperties properties,
            Clock clock,
            Validator validator,
            AuditTrail audit) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mail = mail;
        this.properties = properties;
        this.clock = clock;
        this.validator = validator;
        this.audit=audit;
    }

    private void validate(InitialAdminForm form) {
        if (!validator.validate(form).isEmpty()
                || !Objects.equals(form.getPassword(), form.getPasswordConfirmation()))
            throw new AuthOperationException("Revisa los datos y la confirmación de contraseña.");
    }

    @Override
    public void register(RegistrationForm form) {
        throw new AuthOperationException("El registro de clientes no está disponible en esta versión.");
    }

    private void requireAdministrator(long id) {
        UserAccount actor =
                users.findById(id)
                        .orElseThrow(
                                () -> new AuthOperationException("La sesión ya no es válida."));
        if (!actor.active() || !actor.emailVerified() || actor.role() != UserRole.ADMINISTRADOR)
            throw new AuthOperationException("Esta acción requiere una cuenta administradora.");
    }

    private AuthOperationException duplicate() {
        return new AuthOperationException(
                "No pudimos crear la cuenta con esos datos. Prueba otro usuario o recupera tu"
                    + " cuenta si ya estás registrado.");
    }

    @Transactional
    public void requestVerification(String email) {
        users.findByEmail(email)
                .ifPresent(
                        candidate -> {
                            UserAccount user = users.lockById(candidate.id()).orElseThrow();
                            if (user.active() && !user.emailVerified()) issue(user, VERIFY);
                        });
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmail(email)
                .ifPresent(
                        candidate -> {
                            UserAccount user = users.lockById(candidate.id()).orElseThrow();
                            if (user.active() && user.emailVerified()) issue(user, RESET);
                        });
    }

    @Transactional
    public void requestAccess(String email) {
        users.findByEmail(email)
                .ifPresent(
                        candidate -> {
                            UserAccount user = users.lockById(candidate.id()).orElseThrow();
                            if (user.active()) issue(user, user.emailVerified() ? RESET : VERIFY);
                        });
    }

    private void issue(UserAccount user, String purpose) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        tokens.invalidate(user.id(), purpose, now);
        Duration ttl =
                VERIFY.equals(purpose) ? properties.verificationTtl() : properties.resetTtl();
        tokens.create(user.id(), hash(raw), purpose, now.plus(ttl));
        String link =
                properties.baseUrl().toString().replaceAll("/$", "")
                        + (VERIFY.equals(purpose)
                                ? "/verificar-correo?token="
                                : "/restablecer-clave?token=")
                        + raw;
        mail.send(
                user.email(),
                VERIFY.equals(purpose)
                        ? "Verifica tu cuenta de Angur Gestión"
                        : "Restablece tu contraseña de Angur Gestión",
                "Hola, "
                        + user.name()
                        + ".\n\n"
                        + (VERIFY.equals(purpose)
                                ? "Confirma tu correo para activar el acceso:"
                                : "Elige una nueva contraseña:")
                        + "\n"
                        + link
                        + "\n\nEl enlace vence en "
                        + ttl.toMinutes()
                        + " minutos y solo puede usarse una vez.\n"
                        + "Si no solicitaste este correo, puedes ignorarlo.");
    }

    public static String hash(String raw) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private boolean shape(String raw) {
        return raw != null && raw.matches("[A-Za-z0-9_-]{43}");
    }

    public boolean isTokenValid(String raw, String purpose) {
        if (!shape(raw)) return false;
        return tokens.find(hash(raw), purpose)
                .filter(t -> t.usableAt(clock.instant()))
                .flatMap(t -> users.findById(t.userId()))
                .filter(
                        u ->
                                u.active()
                                        && (VERIFY.equals(purpose)
                                                ? !u.emailVerified()
                                                : u.emailVerified()))
                .isPresent();
    }

    private AuthOperationException invalidToken() {
        return new AuthOperationException(
                "El enlace no es válido, ya se utilizó o ha vencido. Solicita uno nuevo.");
    }

    private UserAccount lockValidToken(String raw, String purpose) {
        if (!shape(raw)) throw invalidToken();
        AuthToken candidate = tokens.find(hash(raw), purpose).orElseThrow(this::invalidToken);
        UserAccount user = users.lockById(candidate.userId()).orElseThrow(this::invalidToken);
        AuthToken token = tokens.lock(hash(raw), purpose).orElseThrow(this::invalidToken);
        if (!token.usableAt(clock.instant())
                || !user.active()
                || (VERIFY.equals(purpose) ? user.emailVerified() : !user.emailVerified()))
            throw invalidToken();
        return user;
    }

    @Transactional
    public void verifyEmail(String raw) {
        UserAccount user = lockValidToken(raw, VERIFY);
        users.verifyEmail(user.id());
        tokens.invalidate(user.id(), VERIFY, clock.instant());
        audit.record(user.id(),"CUENTA",Long.toString(user.id()),"VERIFICAR_CORREO","La cuenta confirmó su correo electrónico.");
    }

    @Transactional
    public void resetPassword(ResetPasswordForm form) {
        if (!validator.validate(form).isEmpty()
                || !Objects.equals(form.getPassword(), form.getPasswordConfirmation()))
            throw new AuthOperationException("Revisa la nueva contraseña y su confirmación.");
        UserAccount user = lockValidToken(form.getToken(), RESET);
        users.changePassword(user.id(), encoder.encode(form.getPassword()));
        tokens.invalidate(user.id(), RESET, clock.instant());
        audit.record(user.id(),"CUENTA",Long.toString(user.id()),"CAMBIAR_CLAVE","Se actualizó la contraseña desde recuperación de acceso.");
    }

    public List<UserAccount> accounts() {
        return users.findAll().stream().filter(u -> u.role() == UserRole.ADMINISTRADOR).toList();
    }

    public boolean isPrincipal(long id) { return users.isPrincipalAdministrator(id); }

    @Transactional
    public void setPrincipal(long id, long administratorId) {
        users.lockSetup();
        requireAdministrator(administratorId);
        if (!users.isPrincipalAdministrator(administratorId))
            throw new AuthOperationException("Solo el administrador principal puede transferir ese control.");
        UserAccount target = users.lockById(id).filter(u -> u.active() && u.emailVerified() && u.role()==UserRole.ADMINISTRADOR)
                .orElseThrow(() -> new AuthOperationException("El administrador elegido debe estar activo y tener el correo verificado."));
        users.setPrincipalAdministrator(target.id());
        audit.record(administratorId,"CUENTA",Long.toString(target.id()),"DESIGNAR_PRINCIPAL","Se designó el administrador principal de la instalación.");
    }

    @Transactional
    public void setActive(long id, boolean active, long administratorId) {
        users.lockSetup();
        requireAdministrator(administratorId);
        UserAccount user =
                users.lockById(id)
                        .orElseThrow(() -> new AuthOperationException("No se encontró la cuenta."));
        if (!active && users.isPrincipalAdministrator(id))
            throw new AuthOperationException("Transfiere el puesto de administrador principal antes de desactivar esta cuenta.");
        if (!active && id == administratorId)
            throw new AuthOperationException("No puedes desactivar tu propia cuenta.");
        if (!active
                && user.role() == UserRole.ADMINISTRADOR
                && users.findAll().stream()
                                .filter(
                                        u ->
                                                u.active()
                                                        && u.emailVerified()
                                                        && u.role() == UserRole.ADMINISTRADOR)
                                .count()
                        <= 1)
            throw new AuthOperationException(
                    "Debe permanecer al menos un administrador activo y verificado.");
        users.setActive(id, active);
        tokens.invalidate(id, RESET, clock.instant());
        tokens.invalidate(id, VERIFY, clock.instant());
        audit.record(administratorId,"CUENTA",Long.toString(id),active?"ACTIVAR":"DESACTIVAR",active?"Se activó una cuenta administradora.":"Se desactivó una cuenta administradora.");
    }
}
