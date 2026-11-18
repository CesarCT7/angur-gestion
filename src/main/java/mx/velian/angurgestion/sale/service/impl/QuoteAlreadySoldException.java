package mx.velian.angurgestion.sale.service.impl;

public class QuoteAlreadySoldException extends RuntimeException {
    private final long saleId;

    public QuoteAlreadySoldException(long saleId) {
        super("Esta cotización ya tiene una venta registrada (#" + saleId + ").");
        this.saleId = saleId;
    }

    public long getSaleId() { return saleId; }
}
