package mx.velian.angurgestion.auth.service.impl;

import mx.velian.angurgestion.auth.config.AuthProperties;

import org.slf4j.*;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AuthMailService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthMailService.class);
    private final JavaMailSender sender;
    private final AuthProperties properties;

    public AuthMailService(JavaMailSender sender, AuthProperties properties) {
        this.sender = sender;
        this.properties = properties;
    }

    public void send(String email, String subject, String text) {
        if (!properties.mailEnabled())
            throw new AuthOperationException(
                    "El envío de correo todavía no está disponible. Contacta a la administración.");
        var message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
        } catch (MailException error) {
            LOG.warn(
                    "No se pudo enviar correo de acceso; revisa SMTP ({}).",
                    error.getClass().getSimpleName());
            throw new AuthOperationException(
                    "No pudimos enviar el correo. Inténtalo más tarde.", error);
        }
    }
}
