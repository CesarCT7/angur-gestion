package mx.velian.angurgestion.auth.service.impl;

import jakarta.validation.Validator;
import mx.velian.angurgestion.auth.config.AuthProperties;
import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.dto.AdminRequestForm;
import mx.velian.angurgestion.auth.dto.ResetPasswordForm;
import mx.velian.angurgestion.auth.model.UserAccount;
import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.audit.service.AuditTrail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class AdminRequestService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final AuthMailService mail;
    private final AuthProperties properties;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final Clock clock;
    private final AuditTrail audit;
    private final SecureRandom random = new SecureRandom();

    public AdminRequestService(JdbcTemplate jdbc, UserRepository users, AuthMailService mail,
            AuthProperties properties, PasswordEncoder encoder, Validator validator, Clock clock, AuditTrail audit) {
        this.jdbc=jdbc; this.users=users; this.mail=mail; this.properties=properties;
        this.encoder=encoder; this.validator=validator; this.clock=clock; this.audit=audit;
    }

    private record Request(long id, String username, String email, String name, long principalId,
            String state, String approvalHash, Instant approvalExpiry, String registrationHash,
            Instant registrationExpiry) {}
    private static final org.springframework.jdbc.core.RowMapper<Request> MAP = (rs,n) ->
            new Request(rs.getLong("id"),rs.getString("usuario"),rs.getString("correo"),
                    rs.getString("nombre"),rs.getLong("administrador_principal_id"),rs.getString("estado"),
                    rs.getString("token_aprobacion_hash"),rs.getTimestamp("token_aprobacion_vence_en").toInstant(),
                    rs.getString("token_registro_hash"),
                    rs.getTimestamp("token_registro_vence_en") == null ? null : rs.getTimestamp("token_registro_vence_en").toInstant());

    @Transactional
    public void request(AdminRequestForm form, long requesterId) {
        if (!validator.validate(form).isEmpty()) throw new AuthOperationException("Revisa los datos de la solicitud.");
        UserAccount requester = users.findById(requesterId).filter(u -> u.active() && u.emailVerified() && u.role()==UserRole.ADMINISTRADOR)
                .orElseThrow(() -> new AuthOperationException("Se requiere una sesión administradora activa."));
        UserAccount principal = users.findPrincipalAdministrator().orElseThrow(() -> new AuthOperationException("No hay administrador principal configurado."));
        if (users.existsByUsernameOrEmail(form.getUsername(), form.getEmail())) throw new AuthOperationException("Ese usuario o correo ya está registrado.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM solicitudes_admin WHERE estado IN ('PENDIENTE','APROBADA') AND (LOWER(usuario)=LOWER(?) OR LOWER(correo)=LOWER(?))", Integer.class, form.getUsername(), form.getEmail()) > 0)
            throw new AuthOperationException("Ya existe una solicitud pendiente para esos datos.");
        String token = token();
        Instant expiry = clock.instant().plus(Duration.ofHours(24));
        jdbc.update("INSERT INTO solicitudes_admin(usuario,correo,nombre,solicitado_por,administrador_principal_id,token_aprobacion_hash,token_aprobacion_vence_en) VALUES (?,?,?,?,?,?,?)",
                form.getUsername().trim().toLowerCase(Locale.ROOT),form.getEmail().trim().toLowerCase(Locale.ROOT),form.getName().trim(),requester.id(),principal.id(),AccountServiceImpl.hash(token),Timestamp.from(expiry));
        Long requestId=jdbc.queryForObject("SELECT id FROM solicitudes_admin WHERE token_aprobacion_hash=?",Long.class,AccountServiceImpl.hash(token));
        audit.record(requester.id(),"SOLICITUD_ADMIN",Long.toString(requestId),"SOLICITAR","Se envió una solicitud de alta administrativa al principal.");
        String link = base()+"/aprobar-administrador?token="+token;
        mail.send(principal.email(), "Solicitud para crear administrador - Angur Gestión",
                "Hola, "+principal.name()+".\n\n"+requester.name()+" solicita crear una cuenta administradora para "+form.getName()+" ("+form.getEmail()+").\n\nAprueba o rechaza esta solicitud: "+link+"\n\nEl enlace vence en 24 horas.");
    }

    public boolean approvalValid(String raw) {
        Request r = find(raw, true);
        return r != null && r.state().equals("PENDIENTE") && r.approvalExpiry().isAfter(clock.instant()) && users.isPrincipalAdministrator(r.principalId());
    }
    public Map<String,String> approvalDetails(String raw) {
        Request r = find(raw, true);
        if (!approvalValid(raw)) throw invalid();
        return Map.of("name",r.name(),"email",r.email(),"username",r.username());
    }

    @Transactional
    public void approve(String raw) {
        Request r = lock(raw, true);
        if (!r.state().equals("PENDIENTE") || !r.approvalExpiry().isAfter(clock.instant()) || !users.isPrincipalAdministrator(r.principalId())) throw invalid();
        String registration = token();
        Instant expiry = clock.instant().plus(Duration.ofHours(48));
        jdbc.update("UPDATE solicitudes_admin SET estado='APROBADA', token_registro_hash=?, token_registro_vence_en=?, resuelto_en=CURRENT_TIMESTAMP(6) WHERE id=?",AccountServiceImpl.hash(registration),Timestamp.from(expiry),r.id());
        audit.record(r.principalId(),"SOLICITUD_ADMIN",Long.toString(r.id()),"APROBAR","Se aprobó una solicitud de alta administrativa.");
        mail.send(r.email(),"Completa tu acceso de administrador - Angur Gestión",
                "Hola, "+r.name()+".\n\nTu solicitud fue aprobada. Define tu contraseña para activar tu cuenta: "+base()+"/completar-admin?token="+registration+"\n\nEl enlace vence en 48 horas y solo puede usarse una vez.");
    }
    @Transactional
    public void reject(String raw) {
        Request r = lock(raw, true);
        if (!r.state().equals("PENDIENTE") || !r.approvalExpiry().isAfter(clock.instant()) || !users.isPrincipalAdministrator(r.principalId())) throw invalid();
        jdbc.update("UPDATE solicitudes_admin SET estado='RECHAZADA', resuelto_en=CURRENT_TIMESTAMP(6) WHERE id=?",r.id());
        audit.record(r.principalId(),"SOLICITUD_ADMIN",Long.toString(r.id()),"RECHAZAR","Se rechazó una solicitud de alta administrativa.");
    }
    public boolean registrationValid(String raw) {
        Request r = find(raw, false);
        return r != null && r.state().equals("APROBADA") && r.registrationExpiry()!=null && r.registrationExpiry().isAfter(clock.instant());
    }
    public Map<String,String> registrationDetails(String raw) {
        Request r = find(raw,false);
        if (!registrationValid(raw)) throw invalid();
        return Map.of("name",r.name(),"email",r.email(),"username",r.username());
    }
    @Transactional
    public void complete(ResetPasswordForm form) {
        if (!validator.validate(form).isEmpty() || !Objects.equals(form.getPassword(),form.getPasswordConfirmation())) throw new AuthOperationException("Revisa la contraseña y su confirmación.");
        Request r = lock(form.getToken(),false);
        if (!r.state().equals("APROBADA") || r.registrationExpiry()==null || !r.registrationExpiry().isAfter(clock.instant())) throw invalid();
        if (users.existsByUsernameOrEmail(r.username(),r.email())) throw new AuthOperationException("El usuario o correo ya está asociado a otra cuenta.");
        long id = users.create(r.username(),r.email(),r.name(),encoder.encode(form.getPassword()),UserRole.ADMINISTRADOR,true);
        jdbc.update("UPDATE solicitudes_admin SET estado='COMPLETADA', cuenta_creada_id=?, token_registro_hash=NULL, token_registro_vence_en=NULL, resuelto_en=CURRENT_TIMESTAMP(6) WHERE id=?",id,r.id());
        audit.record(id,"CUENTA",Long.toString(id),"CREAR_ADMINISTRADOR","Se completó una invitación administrativa aprobada.");
    }

    private Request find(String raw, boolean approval) {
        if (raw==null || !raw.matches("[A-Za-z0-9_-]{43}")) return null;
        String col=approval?"token_aprobacion_hash":"token_registro_hash";
        return jdbc.query("SELECT * FROM solicitudes_admin WHERE "+col+"=?",MAP,AccountServiceImpl.hash(raw)).stream().findFirst().orElse(null);
    }
    private Request lock(String raw, boolean approval) {
        if (raw==null || !raw.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        String col=approval?"token_aprobacion_hash":"token_registro_hash";
        return jdbc.query("SELECT * FROM solicitudes_admin WHERE "+col+"=? FOR UPDATE",MAP,AccountServiceImpl.hash(raw)).stream().findFirst().orElseThrow(this::invalid);
    }
    private String token() { byte[] bytes=new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String base() { return properties.baseUrl().toString().replaceAll("/$",""); }
    private AuthOperationException invalid() { return new AuthOperationException("El enlace no es válido, ya se utilizó o venció. Solicita una nueva aprobación."); }
}
