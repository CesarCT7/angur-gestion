package mx.velian.angurgestion.audit.service;

import mx.velian.angurgestion.audit.dao.AuditRepository;
import mx.velian.angurgestion.auth.model.AngurUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditTrail {
    private final AuditRepository repository;
    public AuditTrail(AuditRepository repository) { this.repository = repository; }

    public void recordCurrent(String entityType, String entityKey, String action, String description) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AngurUserDetails actor)
            record(actor.getId(), entityType, entityKey, action, description);
    }

    public void record(long actorId, String entityType, String entityKey, String action, String description) {
        repository.insert(actorId, clean(entityType, 30), clean(entityKey, 100), clean(action, 30), clean(description, 500));
    }

    private String clean(String value, int max) {
        if (value == null || value.isBlank()) return "";
        String result = value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return result.length() <= max ? result : result.substring(0, max);
    }
}
