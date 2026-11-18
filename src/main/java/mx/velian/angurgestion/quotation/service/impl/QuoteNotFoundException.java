package mx.velian.angurgestion.quotation.service.impl;

public class QuoteNotFoundException extends RuntimeException {
    public QuoteNotFoundException(long id) { super("No se encontró la cotización " + id + "."); }
}
