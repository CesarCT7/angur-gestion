package mx.velian.angurgestion.auth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import mx.velian.angurgestion.auth.dao.UserRepository;
import mx.velian.angurgestion.auth.model.*;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AccountSessionFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public AccountSessionFilter(UserRepository users) {
        this.users = users;
    }

    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/vendor/")
                || path.startsWith("/images/");
    }

    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AngurUserDetails principal) {
            try {
                UserAccount user = users.findById(principal.getId()).orElse(null);
                if (user == null
                        || !user.active()
                        || !user.emailVerified()
                        || user.sessionVersion() != principal.getSessionVersion()
                        || user.role() != principal.getRole()) {
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                    SecurityContextHolder.clearContext();
                    Cookie cookie = new Cookie("JSESSIONID", "");
                    cookie.setMaxAge(0);
                    cookie.setPath(
                            request.getContextPath().isEmpty() ? "/" : request.getContextPath());
                    cookie.setHttpOnly(true);
                    cookie.setSecure(request.isSecure());
                    response.addCookie(cookie);
                    response.sendRedirect(request.getContextPath() + "/login?expired");
                    return;
                }
            } catch (DataAccessException unavailable) {
                response.sendError(503, "No pudimos comprobar tu sesión. Inténtalo más tarde.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
