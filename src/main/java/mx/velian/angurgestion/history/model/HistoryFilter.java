package mx.velian.angurgestion.history.model;

import java.time.LocalDate;

public record HistoryFilter(LocalDate from, LocalDate to, String type, String status,
                            String product, String actor, String search) {}
