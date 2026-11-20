package mx.velian.angurgestion.auth.security;

import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

@Component
public class AttemptLimiter {
    private record Window(Instant until, int count) {}

    private final Map<String, Window> entries = new LinkedHashMap<>();
    private final Clock clock;

    public AttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean allow(String key, int limit, Duration duration) {
        Instant now = clock.instant();
        entries.entrySet().removeIf(e -> !e.getValue().until().isAfter(now));
        Window window = entries.get(key);
        if (window == null) {
            if (entries.size() >= 10000) return false;
            window = new Window(now.plus(duration), 0);
        }
        if (window.count() >= limit) return false;
        entries.put(key, new Window(window.until(), window.count() + 1));
        return true;
    }
}
