package mx.velian.angurgestion.auth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import mx.velian.angurgestion.auth.service.impl.AccountServiceImpl;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class LoginAttemptFilter extends OncePerRequestFilter {
    private final AttemptLimiter limiter;

    public LoginAttemptFilter(AttemptLimiter limiter) {
        this.limiter = limiter;
    }

    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if ("POST".equals(request.getMethod())
                && request.getRequestURI().equals(request.getContextPath() + "/login")) {
            String identifier = request.getParameter("identifier"),
                    password = request.getParameter("password");
            if (identifier == null
                    || identifier.isBlank()
                    || identifier.length() > 254
                    || password == null
                    || password.isBlank()
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
            String account = AccountServiceImpl.hash(identifier.trim().toLowerCase(Locale.ROOT));
            if (!limiter.allow("login-ip:" + request.getRemoteAddr(), 50, Duration.ofMinutes(15))
                    || !limiter.allow("login-account:" + account, 10, Duration.ofMinutes(15))) {
                response.sendRedirect(request.getContextPath() + "/login?limited");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
