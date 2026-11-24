package mx.velian.angurgestion.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.dto.*;
import mx.velian.angurgestion.auth.model.*;
import mx.velian.angurgestion.auth.service.*;
import mx.velian.angurgestion.auth.service.impl.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:tc:mysql:8.4:///angur_gestion?TC_INITSCRIPT=db/schema.sql",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "spring.datasource.username=test",
            "spring.datasource.password=test",
            "angur.auth.mail-enabled=true"
        })
@AutoConfigureMockMvc
class AuthFlowIntegrationTests {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry p) {
        String url = System.getenv("ANGUR_TEST_DB_URL");
        if (url != null && !url.isBlank()) {
            String path = java.net.URI.create(url.substring(5)).getPath();
            if (path == null || !path.endsWith("_test"))
                throw new IllegalArgumentException(
                        "Usa una base exclusiva cuyo nombre termine en _test.");
            p.add("spring.datasource.url", () -> url);
            p.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            p.add("spring.datasource.username", () -> System.getenv("ANGUR_TEST_DB_USER"));
            p.add("spring.datasource.password", () -> System.getenv("ANGUR_TEST_DB_PASSWORD"));
        }
    }

    static final String PASSWORD = "Clave-de-prueba-2026";
    static final AtomicInteger IP = new AtomicInteger();
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired InitialAdminService setup;
    @Autowired AccountService accounts;
    @Autowired PasswordEncoder encoder;
    @MockitoBean JavaMailSender sender;
    String ip;

    @BeforeEach
    void clean() {
        for (String table :
                List.of(
                        "auth_tokens",
                        "movimientos_inventario",
                        "venta_partidas",
                        "ventas",
                        "cotizacion_partidas",
                        "cotizaciones",
                        "existencias",
                        "productos",
                        "clientes",
                        "usuarios")) jdbc.update("DELETE FROM " + table);
        jdbc.update("UPDATE auth_setup SET completado=FALSE WHERE id=1");
        reset(sender);
        ip = "192.0.2." + IP.incrementAndGet();
    }

    MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder r) {
        return r.with(
                q -> {
                    q.setRemoteAddr(ip);
                    return q;
                });
    }

    InitialAdminForm adminForm(String name) {
        var f = new InitialAdminForm();
        f.setName("Administración de prueba");
        f.setUsername(name);
        f.setEmail(name + "@example.test");
        f.setPassword(PASSWORD);
        f.setPasswordConfirmation(PASSWORD);
        return f;
    }

    void admin() {
        setup.createInitialAdministrator(adminForm("admin"));
    }

    RegistrationForm client() {
        var f = new RegistrationForm();
        f.setName("Cliente de prueba");
        f.setUsername("cliente");
        f.setEmail("cliente@example.test");
        f.setPassword(PASSWORD);
        f.setPasswordConfirmation(PASSWORD);
        return f;
    }

    String token() {
        var capture = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, atLeastOnce()).send(capture.capture());
        var match =
                Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(capture.getValue().getText());
        assertThat(match.find()).isTrue();
        return match.group(1);
    }

    UserAccount verified() {
        accounts.register(client());
        accounts.verifyEmail(token());
        return users.findByIdentifier("cliente").orElseThrow();
    }

    MockHttpSession login(String identifier, String password) throws Exception {
        var before = new MockHttpSession();
        String id = before.getId();
        var result =
                mvc.perform(
                                request(
                                        post("/login")
                                                .with(csrf())
                                                .session(before)
                                                .with(
                                                        r -> {
                                                            r.setRequestedSessionId(id);
                                                            r.setRequestedSessionIdValid(true);
                                                            return r;
                                                        })
                                                .param("identifier", identifier)
                                                .param("password", password)))
                        .andExpect(redirectedUrl("/"))
                        .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotEqualTo(id);
        return session;
    }

    @Test
    void publicFormsRenderAndRequireCsrf() throws Exception {
        for (String path :
                List.of(
                        "/login",
                        "/setup",
                        "/registro",
                        "/recuperar-clave",
                        "/reenviar-verificacion",
                        "/verificar-correo",
                        "/restablecer-clave"))
            mvc.perform(request(get(path))).andExpect(status().isOk());
        for (String path :
                List.of(
                        "/setup",
                        "/registro",
                        "/login",
                        "/recuperar-clave",
                        "/reenviar-verificacion",
                        "/verificar-correo",
                        "/restablecer-clave",
                        "/logout"))
            mvc.perform(request(post(path))).andExpect(status().isForbidden());
        for (String path : List.of("/js/auth.js", "/vendor/bootstrap/bootstrap.min.css"))
            mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void setupValidatesAndClosesAfterFirstAccount() throws Exception {
        mvc.perform(request(post("/setup").with(csrf()).param("name", "")))
                .andExpect(status().isOk())
                .andExpect(
                        model().attributeHasFieldErrors(
                                        "form", "name", "username", "email", "password"));
        admin();
        assertThat(users.countUsers()).isEqualTo(1);
        assertThat(users.findByIdentifier("admin").orElseThrow().emailVerified()).isTrue();
        assertThatThrownBy(() -> setup.createInitialAdministrator(adminForm("otro")))
                .isInstanceOf(IllegalStateException.class);
        mvc.perform(get("/setup")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void simultaneousSetupCreatesOnlyOneAdministrator() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int index = i;
                results.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    try {
                                        setup.createInitialAdministrator(
                                                adminForm("admin" + index));
                                        return true;
                                    } catch (IllegalStateException expected) {
                                        return false;
                                    }
                                }));
            }
            start.countDown();
            int accepted = 0;
            for (var result : results) if (result.get(20, TimeUnit.SECONDS)) accepted++;
            assertThat(accepted).isEqualTo(1);
            assertThat(users.countUsers()).isEqualTo(1);
        }
    }

    @Test
    void publicRegistrationCannotEscalatePrivileges() throws Exception {
        admin();
        mvc.perform(
                        request(
                                post("/registro")
                                        .with(csrf())
                                        .param("name", "Cliente de prueba")
                                        .param("username", "cliente")
                                        .param("email", "cliente@example.test")
                                        .param("password", PASSWORD)
                                        .param("passwordConfirmation", PASSWORD)
                                        .param("clientType", "EMPRESA")
                                        .param("role", "ADMINISTRADOR")
                                        .param("emailVerified", "true")))
                .andExpect(redirectedUrl("/login?registered"));
        var user = users.findByIdentifier("cliente").orElseThrow();
        assertThat(user.role()).isEqualTo(UserRole.CLIENTE);
        assertThat(user.emailVerified()).isFalse();
        assertThat(encoder.matches(PASSWORD, user.passwordHash())).isTrue();
        assertThat(user.passwordHash()).doesNotContain(PASSWORD);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM clientes WHERE usuario_id=? AND"
                                    + " tipo='EMPRESA'",
                                Integer.class,
                                user.id()))
                .isEqualTo(1);
        String raw = token();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT token_hash FROM auth_tokens WHERE usuario_id=?",
                                String.class,
                                user.id()))
                .isEqualTo(AccountServiceImpl.hash(raw))
                .isNotEqualTo(raw);
        mvc.perform(
                        request(
                                post("/login")
                                        .with(csrf())
                                        .param("identifier", "cliente")
                                        .param("password", PASSWORD)))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void verificationRequiresPostAndCannotBeReplayed() throws Exception {
        admin();
        accounts.register(client());
        String raw = token();
        mvc.perform(get("/verificar-correo").param("token", raw)).andExpect(status().isOk());
        assertThat(users.findByIdentifier("cliente").orElseThrow().emailVerified()).isFalse();
        assertThat(accounts.isTokenValid(raw, AccountServiceImpl.RESET)).isFalse();
        mvc.perform(request(post("/verificar-correo").with(csrf()).param("token", raw)))
                .andExpect(redirectedUrl("/login?verified"));
        assertThatThrownBy(() -> accounts.verifyEmail(raw))
                .isInstanceOf(AuthOperationException.class);
        login("CLIENTE@EXAMPLE.TEST", PASSWORD);
    }

    @Test
    void reissuedAndExpiredVerificationLinksFail() throws Exception {
        admin();
        accounts.register(client());
        String first = token();
        mvc.perform(
                        request(
                                post("/reenviar-verificacion")
                                        .with(csrf())
                                        .param("email", "cliente@example.test")))
                .andExpect(status().isOk());
        String second = token();
        assertThat(second).isNotEqualTo(first);
        assertThat(accounts.isTokenValid(first, AccountServiceImpl.VERIFY)).isFalse();
        jdbc.update(
                "UPDATE auth_tokens SET vence_en=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 SECOND)");
        assertThatThrownBy(() -> accounts.verifyEmail(second))
                .isInstanceOf(AuthOperationException.class);
    }

    @Test
    void unicodeAndMismatchedPasswordsAreFieldErrors() throws Exception {
        admin();
        for (String password : List.of("á".repeat(40), "corta"))
            mvc.perform(
                            request(
                                    post("/registro")
                                            .with(csrf())
                                            .param("name", "Cliente")
                                            .param("username", "cliente")
                                            .param("email", "cliente@example.test")
                                            .param("password", password)
                                            .param("passwordConfirmation", password)
                                            .param("clientType", "PARTICULAR")))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("form", "password"));
        mvc.perform(
                        request(
                                post("/registro")
                                        .with(csrf())
                                        .param("name", "Cliente")
                                        .param("username", "cliente")
                                        .param("email", "cliente@example.test")
                                        .param("password", PASSWORD)
                                        .param("passwordConfirmation", "diferente")
                                        .param("clientType", "PARTICULAR")))
                .andExpect(model().attributeHasFieldErrors("form", "passwordConfirmation"));
        assertThat(users.countUsers()).isEqualTo(1);
        verifyNoInteractions(sender);
    }

    @Test
    void duplicateAndSmtpFailureLeaveNoPartialAccount() {
        admin();
        accounts.register(client());
        assertThatThrownBy(() -> accounts.register(client()))
                .isInstanceOf(AuthOperationException.class);
        var f = client();
        f.setUsername("fallo");
        f.setEmail("fallo@example.test");
        doThrow(new MailSendException("synthetic failure"))
                .when(sender)
                .send(any(SimpleMailMessage.class));
        assertThatThrownBy(() -> accounts.register(f)).isInstanceOf(AuthOperationException.class);
        assertThat(users.findByIdentifier("fallo")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clientes", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void recoveryDoesNotRevealAccountExistence() throws Exception {
        admin();
        verified();
        reset(sender);
        String first =
                mvc.perform(
                                request(
                                        post("/recuperar-clave")
                                                .with(csrf())
                                                .param("email", "cliente@example.test")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getModelAndView()
                        .getModel()
                        .get("successMessage")
                        .toString();
        String second =
                mvc.perform(
                                request(
                                        post("/recuperar-clave")
                                                .with(csrf())
                                                .param("email", "nadie@example.test")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getModelAndView()
                        .getModel()
                        .get("successMessage")
                        .toString();
        assertThat(first).isEqualTo(second);
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetChangesPasswordAndRevokesSessions() throws Exception {
        admin();
        verified();
        var session = login("cliente", PASSWORD);
        accounts.requestPasswordReset("cliente@example.test");
        String raw = token();
        mvc.perform(get("/restablecer-clave").param("token", raw)).andExpect(status().isOk());
        mvc.perform(
                        request(
                                post("/restablecer-clave")
                                        .with(csrf())
                                        .param("token", raw)
                                        .param("password", "Nueva-clave-segura-2026")
                                        .param("passwordConfirmation", "otra")))
                .andExpect(model().attributeHasFieldErrors("form", "passwordConfirmation"));
        assertThat(accounts.isTokenValid(raw, AccountServiceImpl.RESET)).isTrue();
        mvc.perform(
                        request(
                                post("/restablecer-clave")
                                        .with(csrf())
                                        .param("token", raw)
                                        .param("password", "Nueva-clave-segura-2026")
                                        .param("passwordConfirmation", "Nueva-clave-segura-2026")))
                .andExpect(redirectedUrl("/login?passwordChanged"));
        mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/login?expired"));
        assertThat(session.isInvalid()).isTrue();
        assertThat(accounts.isTokenValid(raw, AccountServiceImpl.RESET)).isFalse();
        mvc.perform(
                        request(
                                post("/login")
                                        .with(csrf())
                                        .param("identifier", "cliente")
                                        .param("password", PASSWORD)))
                .andExpect(redirectedUrl("/login?error"));
        login("cliente", "Nueva-clave-segura-2026");
    }

    @Test
    void expiredAndReissuedResetCannotChangePassword() {
        admin();
        var user = verified();
        accounts.requestPasswordReset(user.email());
        String first = token();
        accounts.requestPasswordReset(user.email());
        String second = token();
        var f = new ResetPasswordForm();
        f.setToken(first);
        f.setPassword("Nueva-clave-segura-2026");
        f.setPasswordConfirmation("Nueva-clave-segura-2026");
        assertThatThrownBy(() -> accounts.resetPassword(f))
                .isInstanceOf(AuthOperationException.class);
        jdbc.update(
                "UPDATE auth_tokens SET vence_en=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 SECOND)");
        f.setToken(second);
        assertThatThrownBy(() -> accounts.resetPassword(f))
                .isInstanceOf(AuthOperationException.class);
        assertThat(
                        encoder.matches(
                                PASSWORD, users.findById(user.id()).orElseThrow().passwordHash()))
                .isTrue();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void permissionsForAllFiveProfiles(UserRole role) throws Exception {
        admin();
        long id =
                users.create(
                        "perfil",
                        "perfil@example.test",
                        "Perfil de prueba",
                        encoder.encode(PASSWORD),
                        role,
                        true);
        var session = login("perfil", PASSWORD);
        mvc.perform(get("/").session(session)).andExpect(status().isOk());
        mvc.perform(get("/productos").session(session))
                .andExpect(status().is(role == UserRole.CLIENTE ? 403 : 200));
        mvc.perform(get("/productos/nuevo").session(session))
                .andExpect(
                        status().is(
                                        role == UserRole.ADMINISTRADOR
                                                        || role == UserRole.ENCARGADO_ALMACEN
                                                ? 200
                                                : 403));
        for (String path : List.of("/usuarios", "/usuarios/nuevo"))
            mvc.perform(get(path).session(session))
                    .andExpect(status().is(role == UserRole.ADMINISTRADOR ? 200 : 403));
    }

    @Test
    void administratorCreatesAndDeactivatesStaff() throws Exception {
        admin();
        var session = login("admin", PASSWORD);
        mvc.perform(
                        request(
                                post("/usuarios/nuevo")
                                        .session(session)
                                        .with(csrf())
                                        .param("name", "Vendedor de prueba")
                                        .param("username", "vendedor")
                                        .param("email", "vendedor@example.test")
                                        .param("password", PASSWORD)
                                        .param("passwordConfirmation", PASSWORD)
                                        .param("role", "VENDEDOR")
                                        .param("clientType", "PARTICULAR")))
                .andExpect(redirectedUrl("/usuarios"));
        accounts.verifyEmail(token());
        var seller = users.findByIdentifier("vendedor").orElseThrow();
        var sellerSession = login("vendedor", PASSWORD);
        mvc.perform(
                        request(
                                post("/usuarios/" + seller.id() + "/estado")
                                        .session(session)
                                        .with(csrf())
                                        .param("active", "false")))
                .andExpect(redirectedUrl("/usuarios"));
        mvc.perform(get("/").session(sellerSession)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(
                        request(
                                post("/login")
                                        .with(csrf())
                                        .param("identifier", "vendedor")
                                        .param("password", PASSWORD)))
                .andExpect(redirectedUrl("/login?error"));
        long adminId = users.findByIdentifier("admin").orElseThrow().id();
        assertThatThrownBy(() -> accounts.setActive(adminId, false, adminId))
                .isInstanceOf(AuthOperationException.class);
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        admin();
        var session = login("admin", PASSWORD);
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/usuarios")).andExpect(status().is3xxRedirection());
    }

    @Test
    void repeatedLoginAttemptsAreLimited() throws Exception {
        admin();
        for (int i = 0; i < 11; i++)
            mvc.perform(
                            request(
                                    post("/login")
                                            .with(csrf())
                                            .param("identifier", "inexistente-" + ip)
                                            .param("password", PASSWORD)))
                    .andExpect(redirectedUrl(i < 10 ? "/login?error" : "/login?limited"));
    }
}
