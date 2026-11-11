package mx.velian.angurgestion.config;

import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/images/**", "/favicon.ico", "/login", "/setup", "/acceso-denegado", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/productos/nuevo", "/productos/*/editar").hasAnyRole(
                                "ADMINISTRADOR", "ENCARGADO_ALMACEN")
                        .requestMatchers(HttpMethod.POST, "/productos/**").hasAnyRole(
                                "ADMINISTRADOR", "ENCARGADO_ALMACEN")
                        .requestMatchers("/productos/**").hasAnyRole(
                                "ADMINISTRADOR", "VENDEDOR", "SUPERVISOR_VENTAS", "ENCARGADO_ALMACEN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("identifier")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(exception -> exception.accessDeniedPage("/acceso-denegado"));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
