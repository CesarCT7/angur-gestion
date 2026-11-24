package mx.velian.angurgestion.history.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoryEntry(String type, long id, LocalDateTime date, String status, String actor,
                           String client, String product, String description, BigDecimal amount,
                           String detailUrl) {}
