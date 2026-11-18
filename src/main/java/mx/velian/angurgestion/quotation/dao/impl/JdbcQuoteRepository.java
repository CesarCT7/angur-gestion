package mx.velian.angurgestion.quotation.dao.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.quotation.dao.QuoteRepository;
import mx.velian.angurgestion.quotation.model.QuoteClientOption;
import mx.velian.angurgestion.quotation.model.QuoteDetail;
import mx.velian.angurgestion.quotation.model.QuoteLine;
import mx.velian.angurgestion.quotation.model.QuoteProductOption;
import mx.velian.angurgestion.quotation.model.QuoteStatus;
import mx.velian.angurgestion.quotation.model.QuoteSummary;

@Repository
public class JdbcQuoteRepository implements QuoteRepository {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String HEADER_SELECT = """
            SELECT q.id, q.cliente_id, c.nombre_razon_social AS cliente,
                   c.correo AS cliente_correo, c.telefono AS cliente_telefono,
                   q.vendedor_id, u.nombre AS vendedor, q.estado, q.observaciones,
                   q.emitida_en, q.total, v.id AS venta_id
            FROM cotizaciones q
            JOIN clientes c ON c.id = q.cliente_id
            JOIN usuarios u ON u.id = q.vendedor_id
            LEFT JOIN ventas v ON v.cotizacion_id = q.id
            """;
    private static final RowMapper<QuoteProductOption> PRODUCT_MAPPER = (rs, row) -> new QuoteProductOption(
            rs.getLong("id"), rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio_venta_actual"));
    private final JdbcTemplate jdbc;

    public JdbcQuoteRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<QuoteSummary> findAll(String search, QuoteStatus status, UserRole role, long userId) {
        StringBuilder sql = new StringBuilder("""
                SELECT q.id, q.emitida_en, c.nombre_razon_social AS cliente, u.nombre AS vendedor,
                       q.estado, q.total, v.id AS venta_id
                FROM cotizaciones q
                JOIN clientes c ON c.id = q.cliente_id
                JOIN usuarios u ON u.id = q.vendedor_id
                LEFT JOIN ventas v ON v.cotizacion_id = q.id
                WHERE 1 = 1
                """);
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        addScope(sql, args, role, userId);
        String term = search == null ? "" : search.trim();
        if (!term.isEmpty()) {
            sql.append(" AND (CAST(q.id AS CHAR) LIKE ? OR c.nombre_razon_social LIKE ? OR EXISTS (")
                    .append("SELECT 1 FROM cotizacion_partidas cp WHERE cp.cotizacion_id = q.id ")
                    .append("AND (cp.codigo_producto LIKE ? OR cp.nombre_producto LIKE ?)))");
            String like = "%" + term + "%";
            args.add(like); args.add(like); args.add(like); args.add(like);
        }
        if (status != null) { sql.append(" AND q.estado = ?"); args.add(status.name()); }
        sql.append(" ORDER BY q.emitida_en DESC, q.id DESC");
        return jdbc.query(sql.toString(), (rs, row) -> new QuoteSummary(
                rs.getLong("id"), format(rs, "emitida_en"), rs.getString("cliente"), rs.getString("vendedor"),
                QuoteStatus.valueOf(rs.getString("estado")), rs.getBigDecimal("total"), nullableLong(rs, "venta_id")),
                args.toArray());
    }

    @Override
    public Optional<QuoteDetail> findById(long id, UserRole role, long userId) {
        StringBuilder sql = new StringBuilder(HEADER_SELECT).append(" WHERE q.id = ?");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        args.add(id);
        addScope(sql, args, role, userId);
        List<QuoteDetail> found = jdbc.query(sql.toString(), this::mapDetailHeader, args.toArray());
        if (found.isEmpty()) return Optional.empty();
        QuoteDetail header = found.get(0);
        List<QuoteLine> lines = jdbc.query("""
                SELECT producto_id, codigo_producto, nombre_producto, cantidad, precio_unitario, subtotal
                FROM cotizacion_partidas WHERE cotizacion_id = ? ORDER BY id
                """, (rs, row) -> new QuoteLine(nullableLong(rs, "producto_id"), rs.getString("codigo_producto"),
                rs.getString("nombre_producto"), rs.getLong("cantidad"), rs.getBigDecimal("precio_unitario"),
                rs.getBigDecimal("subtotal")), id);
        return Optional.of(new QuoteDetail(header.id(), header.clientId(), header.clientName(), header.clientEmail(),
                header.clientPhone(), header.sellerId(), header.sellerName(), header.status(), header.issuedAt(),
                header.observations(), header.total(), header.saleId(), lines));
    }

    @Override
    public Optional<QuoteDetail> findAcceptedForSale(long id, UserRole role, long userId) {
        return findById(id, role, userId).filter(quote -> quote.status() == QuoteStatus.ACEPTADA);
    }

    @Override
    public List<QuoteClientOption> findActiveClients() {
        return jdbc.query("""
                SELECT id, nombre_razon_social FROM clientes WHERE activo = TRUE
                ORDER BY nombre_razon_social
                """, (rs, row) -> new QuoteClientOption(rs.getLong("id"), rs.getString("nombre_razon_social")));
    }

    @Override
    public List<QuoteProductOption> findActiveProducts() {
        return jdbc.query("""
                SELECT id, codigo, nombre, precio_venta_actual FROM productos
                WHERE activo = TRUE ORDER BY nombre
                """, PRODUCT_MAPPER);
    }

    @Override
    public Optional<QuoteProductOption> findActiveProduct(long id) {
        return jdbc.query("""
                SELECT id, codigo, nombre, precio_venta_actual FROM productos
                WHERE id = ? AND activo = TRUE
                """, PRODUCT_MAPPER, id).stream().findFirst();
    }

    @Override
    public boolean isActiveClient(long id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM clientes WHERE id = ? AND activo = TRUE", Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public long insertHeader(long clientId, long sellerId, String observations, BigDecimal total) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO cotizaciones (cliente_id, vendedor_id, estado, observaciones, total)
                    VALUES (?, ?, 'PENDIENTE', ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, clientId);
            statement.setLong(2, sellerId);
            statement.setString(3, observations == null || observations.isBlank() ? null : observations.trim());
            statement.setBigDecimal(4, total);
            return statement;
        }, holder);
        Number id = holder.getKey();
        if (id == null) throw new IllegalStateException("No se pudo obtener el folio de la cotización.");
        return id.longValue();
    }

    @Override
    public void insertLine(long quoteId, long productId, String code, String name, long quantity,
                           BigDecimal unitPrice, BigDecimal subtotal) {
        jdbc.update("""
                INSERT INTO cotizacion_partidas
                    (cotizacion_id, producto_id, codigo_producto, nombre_producto, cantidad, precio_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, quoteId, productId, code, name, quantity, unitPrice, subtotal);
    }

    @Override
    public int changePendingStatus(long id, QuoteStatus next, UserRole role, long userId) {
        if (role == UserRole.ADMINISTRADOR) {
            return jdbc.update("UPDATE cotizaciones SET estado = ? WHERE id = ? AND estado = 'PENDIENTE'", next.name(), id);
        }
        return jdbc.update("""
                UPDATE cotizaciones SET estado = ?
                WHERE id = ? AND vendedor_id = ? AND estado = 'PENDIENTE'
                """, next.name(), id, userId);
    }

    private QuoteDetail mapDetailHeader(ResultSet rs, int row) throws SQLException {
        return new QuoteDetail(rs.getLong("id"), rs.getLong("cliente_id"), rs.getString("cliente"),
                rs.getString("cliente_correo"), rs.getString("cliente_telefono"), rs.getLong("vendedor_id"),
                rs.getString("vendedor"), QuoteStatus.valueOf(rs.getString("estado")), format(rs, "emitida_en"),
                rs.getString("observaciones"), rs.getBigDecimal("total"), nullableLong(rs, "venta_id"), List.of());
    }

    private static void addScope(StringBuilder sql, List<Object> args, UserRole role, long userId) {
        if (role == UserRole.VENDEDOR) { sql.append(" AND q.vendedor_id = ?"); args.add(userId); }
        if (role == UserRole.CLIENTE) { sql.append(" AND c.usuario_id = ?"); args.add(userId); }
    }

    private static String format(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toLocalDateTime().format(DATE_FORMAT);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
