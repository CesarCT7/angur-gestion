package mx.velian.angurgestion.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("angur.auth")
public record AuthProperties(
        URI baseUrl,
        String mailFrom,
        boolean mailEnabled,
        Duration verificationTtl,
        Duration resetTtl) {
    public AuthProperties {
        if (baseUrl == null
                || baseUrl.getHost() == null
                || baseUrl.getRawQuery() != null
                || baseUrl.getFragment() != null
                || baseUrl.getUserInfo() != null
                || !("https".equals(baseUrl.getScheme())
                        || ("http".equals(baseUrl.getScheme())
                                && ("localhost".equals(baseUrl.getHost())
                                        || "127.0.0.1".equals(baseUrl.getHost())))))
            throw new IllegalArgumentException(
                    "AUTH_BASE_URL debe usar HTTPS; HTTP solo se permite en localhost.");
        if (verificationTtl == null
                || resetTtl == null
                || verificationTtl.isNegative()
                || verificationTtl.isZero()
                || resetTtl.isNegative()
                || resetTtl.isZero())
            throw new IllegalArgumentException("La duración de los enlaces debe ser positiva.");
    }
}
