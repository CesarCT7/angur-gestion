package mx.velian.angurgestion.config;

import mx.velian.angurgestion.auth.config.AuthProperties;
import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.security.*;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, UserRepository users, AttemptLimiter limiter) throws Exception {
        http.authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/css/**",
                                                "/js/**",
                                                "/vendor/**",
                                                "/images/**",
                                                "/favicon.ico",
                                                "/login",
                                                "/setup",
                                                "/registro",
                                                "/recuperar-acceso",
                                                "/verificar-correo",
                                                "/reenviar-verificacion",
                                                "/recuperar-clave",
                                                "/restablecer-clave",
                                                "/acceso-denegado",
                                                "/error")
                                        .permitAll()
                                        .requestMatchers("/usuarios/**")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers(HttpMethod.POST, "/cotizaciones/**", "/ventas/**")
                                        .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                                        .requestMatchers(HttpMethod.GET, "/cotizaciones/nueva", "/ventas/nueva", "/ventas/nueva/cotizacion/**")
                                        .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                                        .requestMatchers("/cotizaciones/**", "/ventas/**")
                                        .hasAnyRole("ADMINISTRADOR", "VENDEDOR", "SUPERVISOR_VENTAS", "CLIENTE")                        .requestMatchers("/clientes/**")
                                        .hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                                        .requestMatchers(HttpMethod.GET, "/inventario/movimiento")
                                        .hasAnyRole("ADMINISTRADOR", "ENCARGADO_ALMACEN")
                                        .requestMatchers(HttpMethod.POST, "/inventario/**")
                                        .hasAnyRole("ADMINISTRADOR", "ENCARGADO_ALMACEN")
                                        .requestMatchers("/inventario/**")
                                        .hasAnyRole("ADMINISTRADOR", "VENDEDOR", "ENCARGADO_ALMACEN")
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/productos/nuevo",
                                                "/productos/archivados",
                                                "/productos/*/editar")
                                        .hasAnyRole("ADMINISTRADOR", "ENCARGADO_ALMACEN")
                                        .requestMatchers(HttpMethod.POST, "/productos/**")
                                        .hasAnyRole("ADMINISTRADOR", "ENCARGADO_ALMACEN")
                                        .requestMatchers("/productos/**")
                                        .hasAnyRole(
                                                "ADMINISTRADOR",
                                                "VENDEDOR",
                                                "SUPERVISOR_VENTAS",
                                                "ENCARGADO_ALMACEN")
                                        .anyRequest()
                                        .authenticated())
                .formLogin(
                        f ->
                                f.loginPage("/login")
                                        .loginProcessingUrl("/login")
                                        .usernameParameter("identifier")
                                        .defaultSuccessUrl("/", true)
                                        .failureHandler(
                                                (request, response, error) ->
                                                        response.sendRedirect(
                                                                request.getContextPath()
                                                                        + (error
                                                                                        instanceof
                                                                                        AuthenticationServiceException
                                                                                ? "/login?unavailable"
                                                                                : "/login?error")))
                                        .permitAll())
                .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
                .logout(
                        l ->
                                l.logoutUrl("/logout")
                                        .logoutSuccessUrl("/login?logout")
                                        .invalidateHttpSession(true)
                                        .clearAuthentication(true)
                                        .deleteCookies("JSESSIONID"))
                .headers(
                        h ->
                                h.referrerPolicy(
                                        p ->
                                                p.policy(
                                                        ReferrerPolicyHeaderWriter.ReferrerPolicy
                                                                .NO_REFERRER)))
                .exceptionHandling(e -> e.accessDeniedPage("/acceso-denegado"))
                .addFilterAfter(new AccountSessionFilter(users), SecurityContextHolderFilter.class)
                .addFilterBefore(
                        new LoginAttemptFilter(limiter),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
