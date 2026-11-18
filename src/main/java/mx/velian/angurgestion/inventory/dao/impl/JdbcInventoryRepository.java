package mx.velian.angurgestion.inventory.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import mx.velian.angurgestion.inventory.dao.InventoryRepository;
import mx.velian.angurgestion.inventory.model.InventoryItem;
import mx.velian.angurgestion.inventory.model.InventoryMovement;
import mx.velian.angurgestion.inventory.model.MovementType;

@Repository
public class JdbcInventoryRepository implements InventoryRepository {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SELECT_ITEMS = """
            SELECT p.id, p.codigo, p.nombre, p.costo_actual, p.precio_venta_actual,
                   COALESCE(e.cantidad, 0) AS existencia
            FROM productos p LEFT JOIN existencias e ON e.producto_id = p.id
            WHERE p.activo = TRUE
            """;
    private static final RowMapper<InventoryItem> ITEM_MAPPER = JdbcInventoryRepository::mapItem;
    private static final RowMapper<InventoryMovement> MOVEMENT_MAPPER = JdbcInventoryRepository::mapMovement;
    private final JdbcTemplate jdbcTemplate;

    public JdbcInventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<InventoryItem> findItems(String search) {
        String term = search == null ? "" : search.trim();
        if (term.isEmpty()) return jdbcTemplate.query(SELECT_ITEMS + " ORDER BY p.nombre ASC", ITEM_MAPPER);
        String pattern = "%" + term + "%";
        return jdbcTemplate.query(SELECT_ITEMS + " AND (p.codigo LIKE ? OR p.nombre LIKE ?) ORDER BY p.nombre ASC",
                ITEM_MAPPER, pattern, pattern);
    }

    @Override
    public List<InventoryMovement> findRecentMovements(String search, int limit) {
        String term = search == null ? "" : search.trim();
        String sql = """
                SELECT m.id, m.fecha, COALESCE(p.codigo, m.codigo_producto) AS codigo,
                       COALESCE(p.nombre, m.nombre_producto) AS nombre,
                       m.tipo, m.cantidad, m.existencia_resultante, m.motivo, u.nombre AS usuario_nombre
                FROM movimientos_inventario m
                LEFT JOIN productos p ON p.id = m.producto_id
                JOIN usuarios u ON u.id = m.usuario_id
                """;
        if (term.isEmpty()) {
            return jdbcTemplate.query(sql + " ORDER BY m.fecha DESC, m.id DESC LIMIT ?", MOVEMENT_MAPPER, limit);
        }
        String pattern = "%" + term + "%";
        return jdbcTemplate.query(sql + """
                WHERE COALESCE(p.codigo, m.codigo_producto) LIKE ?
                   OR COALESCE(p.nombre, m.nombre_producto) LIKE ?
                   OR m.motivo LIKE ?
                ORDER BY m.fecha DESC, m.id DESC LIMIT ?
                """, MOVEMENT_MAPPER, pattern, pattern, pattern, limit);
    }

    @Override
    public Optional<InventoryMovement> findByOperation(String key, long productId, MovementType type) {
        return jdbcTemplate.query("""
                SELECT m.id, m.fecha, COALESCE(p.codigo, m.codigo_producto) AS codigo,
                       COALESCE(p.nombre, m.nombre_producto) AS nombre,
                       m.tipo, m.cantidad, m.existencia_resultante, m.motivo, u.nombre AS usuario_nombre
                FROM movimientos_inventario m
                LEFT JOIN productos p ON p.id = m.producto_id
                JOIN usuarios u ON u.id = m.usuario_id
                WHERE m.clave_operacion = ? AND m.producto_id = ? AND m.tipo = ?
                """, MOVEMENT_MAPPER, key, productId, type.name()).stream().findFirst();
    }

    @Override
    public Optional<InventoryItem> lockActiveItem(long productId) {
        return jdbcTemplate.query("""
                SELECT p.id, p.codigo, p.nombre, p.costo_actual, p.precio_venta_actual, e.cantidad AS existencia
                FROM productos p JOIN existencias e ON e.producto_id = p.id
                WHERE p.id = ? AND p.activo = TRUE
                FOR UPDATE
                """, ITEM_MAPPER, productId).stream().findFirst();
    }

    @Override
    public int updateStock(long productId, long newStock) {
        return jdbcTemplate.update("UPDATE existencias SET cantidad = ? WHERE producto_id = ?", newStock, productId);
    }

    @Override
    public void insertMovement(long productId, String code, String name, long userId,
            MovementType type, long quantity, long resultingStock, String reason, String key) {
        jdbcTemplate.update("""
                INSERT INTO movimientos_inventario
                    (producto_id, codigo_producto, nombre_producto, usuario_id, tipo, cantidad,
                     existencia_resultante, motivo, clave_operacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, productId, code, name, userId, type.name(), quantity, resultingStock, reason.trim(), key);
    }

    private static InventoryItem mapItem(ResultSet rs, int row) throws SQLException {
        return new InventoryItem(rs.getLong("id"), rs.getString("codigo"), rs.getString("nombre"),
                rs.getBigDecimal("costo_actual"), rs.getBigDecimal("precio_venta_actual"), rs.getLong("existencia"));
    }

    private static InventoryMovement mapMovement(ResultSet rs, int row) throws SQLException {
        return new InventoryMovement(rs.getLong("id"), rs.getTimestamp("fecha").toLocalDateTime().format(DATE_FORMAT),
                rs.getString("codigo"), rs.getString("nombre"), MovementType.valueOf(rs.getString("tipo")),
                rs.getLong("cantidad"), rs.getLong("existencia_resultante"), rs.getString("motivo"),
                rs.getString("usuario_nombre"));
    }
}
