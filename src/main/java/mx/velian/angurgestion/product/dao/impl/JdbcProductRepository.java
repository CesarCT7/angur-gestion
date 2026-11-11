package mx.velian.angurgestion.product.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import mx.velian.angurgestion.product.dao.ProductRepository;
import mx.velian.angurgestion.product.dto.ProductForm;
import mx.velian.angurgestion.product.model.Product;

@Repository
public class JdbcProductRepository implements ProductRepository {
    private static final String SELECT_PRODUCTS = """
            SELECT p.id, p.codigo, p.nombre, p.descripcion,
                   p.costo_actual, p.precio_venta_actual,
                   COALESCE(e.cantidad, 0) AS existencia, p.activo
            FROM productos p
            LEFT JOIN existencias e ON e.producto_id = p.id
            """;
    private static final RowMapper<Product> PRODUCT_MAPPER = JdbcProductRepository::mapProduct;
    private final JdbcTemplate jdbcTemplate;

    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Product> findAll(String search) {
        String term = search == null ? "" : search.trim();
        if (term.isEmpty()) {
            return jdbcTemplate.query(
                    SELECT_PRODUCTS + " ORDER BY p.activo DESC, p.nombre ASC", PRODUCT_MAPPER);
        }
        String pattern = "%" + term + "%";
        return jdbcTemplate.query(SELECT_PRODUCTS + """
                WHERE p.codigo LIKE ? OR p.nombre LIKE ?
                ORDER BY p.activo DESC, p.nombre ASC
                """, PRODUCT_MAPPER, pattern, pattern);
    }

    @Override
    public Optional<Product> findById(long id) {
        List<Product> result = jdbcTemplate.query(
                SELECT_PRODUCTS + " WHERE p.id = ?", PRODUCT_MAPPER, id);
        return result.stream().findFirst();
    }

    @Override
    public boolean existsByCode(String code) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM productos WHERE LOWER(codigo) = LOWER(?)",
                Integer.class, code.trim());
        return count != null && count > 0;
    }

    @Override
    public boolean existsByCodeExceptId(String code, long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM productos WHERE LOWER(codigo) = LOWER(?) AND id <> ?",
                Integer.class, code.trim(), id);
        return count != null && count > 0;
    }

    @Override
    public long countActiveProducts() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM productos WHERE activo = TRUE", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long countActiveUnits() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(e.cantidad), 0)
                FROM existencias e
                JOIN productos p ON p.id = e.producto_id
                WHERE p.activo = TRUE
                """, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public void insert(ProductForm form) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO productos (codigo, nombre, descripcion, costo_actual, precio_venta_actual)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, form.getCode().trim());
            statement.setString(2, form.getName().trim());
            statement.setString(3, normalizeDescription(form.getDescription()));
            statement.setBigDecimal(4, form.getCost());
            statement.setBigDecimal(5, form.getSalePrice());
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("No se pudo obtener el identificador del producto.");
        }
        jdbcTemplate.update("INSERT INTO existencias (producto_id, cantidad) VALUES (?, 0)", generatedId.longValue());
    }

    @Override
    public int update(long id, ProductForm form) {
        return jdbcTemplate.update("""
                UPDATE productos
                SET codigo = ?, nombre = ?, descripcion = ?, costo_actual = ?, precio_venta_actual = ?
                WHERE id = ? AND activo = TRUE
                """, form.getCode().trim(), form.getName().trim(), normalizeDescription(form.getDescription()),
                form.getCost(), form.getSalePrice(), id);
    }

    @Override
    public int deactivate(long id) {
        return jdbcTemplate.update("UPDATE productos SET activo = FALSE WHERE id = ? AND activo = TRUE", id);
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private static Product mapProduct(ResultSet rs, int rowNumber) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getBigDecimal("costo_actual"),
                rs.getBigDecimal("precio_venta_actual"),
                rs.getLong("existencia"),
                rs.getBoolean("activo"));
    }
}
