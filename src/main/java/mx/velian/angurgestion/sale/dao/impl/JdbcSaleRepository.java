package mx.velian.angurgestion.sale.dao.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import mx.velian.angurgestion.auth.model.UserRole;
import mx.velian.angurgestion.sale.dao.SaleRepository;
import mx.velian.angurgestion.sale.model.SaleDetail;
import mx.velian.angurgestion.sale.model.SaleLine;
import mx.velian.angurgestion.sale.model.SaleLine;
import mx.velian.angurgestion.sale.model.SaleOperation;
import mx.velian.angurgestion.sale.model.SaleStatus;
import mx.velian.angurgestion.sale.model.SaleSummary;

@Repository
public class JdbcSaleRepository implements SaleRepository {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SALE_SELECT = """
            SELECT v.id, v.cliente_id, COALESCE(c.nombre_razon_social, 'Sin cliente') AS cliente,
                   v.cotizacion_id, v.vendedor_id, u.nombre AS vendedor, v.estado, v.fecha, v.total
            FROM ventas v
            LEFT JOIN clientes c ON c.id = v.cliente_id
            JOIN usuarios u ON u.id = v.vendedor_id
            """;
    private final JdbcTemplate jdbc;

    public JdbcSaleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<SaleSummary> findAll(String search, SaleStatus status, UserRole role, long userId) {
        StringBuilder sql = new StringBuilder("""
                SELECT v.id, v.fecha, COALESCE(c.nombre_razon_social, 'Sin cliente') AS cliente, u.nombre AS vendedor,
                       v.cotizacion_id, v.estado, v.total
                FROM ventas v
                LEFT JOIN clientes c ON c.id = v.cliente_id
                JOIN usuarios u ON u.id = v.vendedor_id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        addScope(sql, args, role, userId);
        String term = search == null ? "" : search.trim();
        if (!term.isEmpty()) {
            sql.append(" AND (CAST(v.id AS CHAR) LIKE ? OR EXISTS (")
                    .append("SELECT 1 FROM venta_partidas vp WHERE vp.venta_id = v.id ")
                    .append("AND (vp.codigo_producto LIKE ? OR vp.nombre_producto LIKE ?)))");
            String like = "%" + term + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (status != null) { sql.append(" AND v.estado = ?"); args.add(status.name()); }
        sql.append(" ORDER BY v.fecha DESC, v.id DESC");
        return jdbc.query(sql.toString(), (rs, row) -> new SaleSummary(
                rs.getLong("id"), format(rs, "fecha"), rs.getString("cliente"), rs.getString("vendedor"),
                nullableLong(rs, "cotizacion_id"), SaleStatus.valueOf(rs.getString("estado")), rs.getBigDecimal("total")),
                args.toArray());
    }

    @Override
    public Optional<SaleDetail> findById(long id, UserRole role, long userId) {
        StringBuilder sql = new StringBuilder(SALE_SELECT).append(" WHERE v.id = ?");
        List<Object> args = new ArrayList<>();
        args.add(id);
        addScope(sql, args, role, userId);
        List<SaleDetail> found = jdbc.query(sql.toString(), this::mapHeader, args.toArray());
        if (found.isEmpty()) return Optional.empty();
        SaleDetail header = found.get(0);
        List<SaleLine> lines = jdbc.query("""
                SELECT producto_id, codigo_producto, nombre_producto, cantidad, precio_unitario, costo_unitario, subtotal
                FROM venta_partidas WHERE venta_id = ? ORDER BY id
                """, (rs, row) -> new SaleLine(nullableLong(rs, "producto_id"), rs.getString("codigo_producto"),
                rs.getString("nombre_producto"), rs.getLong("cantidad"), rs.getBigDecimal("precio_unitario"),
                rs.getBigDecimal("costo_unitario"), rs.getBigDecimal("subtotal")), id);
        return Optional.of(new SaleDetail(header.id(), header.clientId(), header.clientName(), header.sellerId(),
                header.sellerName(), header.quoteId(), header.date(), header.status(), header.total(), lines));
    }

    @Override
    public Optional<SaleOperation> findByOperationKey(String key) {
        return operationQuery("SELECT id, vendedor_id, cliente_id, cotizacion_id, total FROM ventas WHERE clave_operacion = ?", key)
                .stream().findFirst();
    }

    @Override
    public Optional<SaleOperation> findCommittedByOperationKey(String key) {
        return operationQuery("SELECT id, vendedor_id, cliente_id, cotizacion_id, total FROM ventas WHERE clave_operacion = ? FOR UPDATE", key)
                .stream().findFirst();
    }

    @Override
    public Optional<SaleOperation> findCommittedByQuoteId(long quoteId) {
        return jdbc.query("""
                SELECT id, vendedor_id, cliente_id, cotizacion_id, total
                FROM ventas WHERE cotizacion_id = ? FOR UPDATE
                """, (rs, row) -> new SaleOperation(rs.getLong("id"), rs.getLong("vendedor_id"),
                nullableLong(rs, "cliente_id"), nullableLong(rs, "cotizacion_id"), rs.getBigDecimal("total")), quoteId)
                .stream().findFirst();
    }

    @Override
    public List<SaleLine> findQuoteLines(long quoteId) {
        return jdbc.query("""
                SELECT producto_id, codigo_producto, nombre_producto, cantidad, precio_unitario, subtotal
                FROM cotizacion_partidas WHERE cotizacion_id = ? ORDER BY id
                """, (rs, row) -> new SaleLine(nullableLong(rs, "producto_id"), rs.getString("codigo_producto"),
                rs.getString("nombre_producto"), rs.getLong("cantidad"), rs.getBigDecimal("precio_unitario"),
                null, rs.getBigDecimal("subtotal")), quoteId);
    }

    @Override
    public long insertHeader(Long clientId, Long quoteId, long sellerId, String key, BigDecimal total) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ventas (cliente_id, cotizacion_id, vendedor_id, clave_operacion, estado, total)
                    VALUES (?, ?, ?, ?, 'REGISTRADA', ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            if (clientId == null) statement.setNull(1, java.sql.Types.BIGINT); else statement.setLong(1, clientId);
            if (quoteId == null) statement.setNull(2, java.sql.Types.BIGINT); else statement.setLong(2, quoteId);
            statement.setLong(3, sellerId);
            statement.setString(4, key);
            statement.setBigDecimal(5, total);
            return statement;
        }, holder);
        Number id = holder.getKey();
        if (id == null) throw new IllegalStateException("No se pudo obtener el folio de la venta.");
        return id.longValue();
    }

    @Override
    public void insertLine(long saleId, long productId, String code, String name, long quantity,
                           BigDecimal unitPrice, BigDecimal unitCost, BigDecimal subtotal) {
        jdbc.update("""
                INSERT INTO venta_partidas
                    (venta_id, producto_id, codigo_producto, nombre_producto, cantidad,
                     precio_unitario, costo_unitario, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, saleId, productId, code, name, quantity, unitPrice, unitCost, subtotal);
    }

    @Override
    public void insertMovement(long saleId, long productId, String code, String name, long userId,
                               long quantity, long resultingStock, String key) {
        jdbc.update("""
                INSERT INTO movimientos_inventario
                    (producto_id, codigo_producto, nombre_producto, usuario_id, venta_id, tipo,
                     cantidad, existencia_resultante, motivo, clave_operacion)
                VALUES (?, ?, ?, ?, ?, 'SALIDA', ?, ?, ?, ?)
                """, productId, code, name, userId, saleId, quantity, resultingStock,
                "Venta #" + saleId, key);
    }

    private List<SaleOperation> operationQuery(String sql, String key) {
        return jdbc.query(sql, (rs, row) -> new SaleOperation(rs.getLong("id"), rs.getLong("vendedor_id"),
                nullableLong(rs, "cliente_id"), nullableLong(rs, "cotizacion_id"), rs.getBigDecimal("total")), key);
    }

    private SaleDetail mapHeader(ResultSet rs, int row) throws SQLException {
        return new SaleDetail(rs.getLong("id"), nullableLong(rs, "cliente_id"), rs.getString("cliente"),
                rs.getLong("vendedor_id"), rs.getString("vendedor"), nullableLong(rs, "cotizacion_id"),
                format(rs, "fecha"), SaleStatus.valueOf(rs.getString("estado")), rs.getBigDecimal("total"), List.of());
    }

    private static void addScope(StringBuilder sql, List<Object> args, UserRole role, long userId) {
        if (role == UserRole.VENDEDOR) { sql.append(" AND v.vendedor_id = ?"); args.add(userId); }
        else if (role != UserRole.ADMINISTRADOR && role != UserRole.SUPERVISOR_VENTAS) { sql.append(" AND 1 = 0"); }
    }

    private static String format(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toLocalDateTime().format(DATE_FORMAT);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
