package mx.velian.angurgestion.dashboard.dao.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import mx.velian.angurgestion.dashboard.dao.DashboardRepository;
import mx.velian.angurgestion.dashboard.model.RecentMovement;

@Repository
public class JdbcDashboardRepository implements DashboardRepository {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final RowMapper<RecentMovement> MOVEMENT_MAPPER = JdbcDashboardRepository::mapMovement;
    private final JdbcTemplate jdbcTemplate;

    public JdbcDashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countRegisteredSales() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ventas WHERE estado = 'REGISTRADA'", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public BigDecimal sumRevenue() {
        BigDecimal sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(vp.subtotal), 0)
                FROM venta_partidas vp
                JOIN ventas v ON v.id = vp.venta_id
                WHERE v.estado = 'REGISTRADA'
                """, BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumCost() {
        BigDecimal sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(vp.costo_unitario * vp.cantidad), 0)
                FROM venta_partidas vp
                JOIN ventas v ON v.id = vp.venta_id
                WHERE v.estado = 'REGISTRADA'
                """, BigDecimal.class);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public List<RecentMovement> findRecentMovements(int limit) {
        return jdbcTemplate.query("""
                SELECT m.fecha, p.codigo, p.nombre, m.tipo, m.cantidad, m.motivo, u.nombre AS usuario_nombre
                FROM movimientos_inventario m
                JOIN productos p ON p.id = m.producto_id
                JOIN usuarios u ON u.id = m.usuario_id
                ORDER BY m.fecha DESC, m.id DESC
                LIMIT ?
                """, MOVEMENT_MAPPER, limit);
    }

    private static RecentMovement mapMovement(ResultSet rs, int rowNumber) throws SQLException {
        return new RecentMovement(
                rs.getTimestamp("fecha").toLocalDateTime().format(DATE_FORMAT),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("tipo"),
                rs.getLong("cantidad"),
                rs.getString("motivo"),
                rs.getString("usuario_nombre"));
    }
}
