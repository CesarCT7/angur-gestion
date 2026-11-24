package mx.velian.angurgestion.report.dao.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import mx.velian.angurgestion.report.dao.ReportsRepository;
import mx.velian.angurgestion.report.model.MonthlyReportRow;
import mx.velian.angurgestion.report.model.ProductReportRow;
import mx.velian.angurgestion.report.model.ReportFilter;
import mx.velian.angurgestion.report.model.ReportSummary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReportsRepository implements ReportsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcReportsRepository(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}

    private static final String MONTH_BUCKET = "DATE_FORMAT(v.fecha, '%Y-%m')";

    private MapSqlParameterSource params(ReportFilter f){
        var p=new MapSqlParameterSource().addValue("from",Timestamp.valueOf(f.from().atStartOfDay()))
                .addValue("to",Timestamp.valueOf(f.to().plusDays(1).atStartOfDay()));
        if(f.product()!=null&&!f.product().isBlank())p.addValue("product","%"+f.product().trim()+"%");
        if(f.seller()!=null&&!f.seller().isBlank())p.addValue("seller","%"+f.seller().trim()+"%");
        return p;
    }
    private String where(ReportFilter f){
        StringBuilder s=new StringBuilder(" WHERE v.estado='REGISTRADA' AND v.fecha>=:from AND v.fecha<:to");
        if(f.product()!=null&&!f.product().isBlank())s.append(" AND (vp.codigo_producto LIKE :product OR vp.nombre_producto LIKE :product)");
        if(f.seller()!=null&&!f.seller().isBlank())s.append(" AND (u.nombre LIKE :seller OR u.usuario LIKE :seller)");
        return s.toString();
    }
    private String from(ReportFilter f){return " FROM ventas v JOIN venta_partidas vp ON vp.venta_id=v.id JOIN usuarios u ON u.id=v.vendedor_id "+where(f);}

    @Override public ReportSummary summarize(ReportFilter f){
        String q="SELECT COUNT(DISTINCT v.id) ventas,COALESCE(SUM(vp.subtotal),0) ingresos,COALESCE(SUM(vp.costo_unitario*vp.cantidad),0) costos,COALESCE(SUM(vp.subtotal-vp.costo_unitario*vp.cantidad),0) ganancia"+from(f);
        return jdbc.queryForObject(q,params(f),(rs,n)->new ReportSummary(rs.getLong("ventas"),zero(rs.getBigDecimal("ingresos")),zero(rs.getBigDecimal("costos")),zero(rs.getBigDecimal("ganancia"))));
    }
    @Override public List<MonthlyReportRow> byMonth(ReportFilter f){
        // Agrupa exactamente por la expresión seleccionada; MySQL con ONLY_FULL_GROUP_BY
        // no acepta seleccionar DATE_FORMAT agrupando por YEAR() y MONTH() por separado.
        String q="SELECT "+MONTH_BUCKET+" mes,COUNT(DISTINCT v.id) ventas,COALESCE(SUM(vp.subtotal),0) ingresos,COALESCE(SUM(vp.costo_unitario*vp.cantidad),0) costos,COALESCE(SUM(vp.subtotal-vp.costo_unitario*vp.cantidad),0) ganancia"+from(f)+" GROUP BY "+MONTH_BUCKET+" ORDER BY mes";
        return jdbc.query(q,params(f),(rs,n)->new MonthlyReportRow(rs.getString("mes"),rs.getLong("ventas"),zero(rs.getBigDecimal("ingresos")),zero(rs.getBigDecimal("costos")),zero(rs.getBigDecimal("ganancia"))));
    }
    @Override public List<ProductReportRow> byProduct(ReportFilter f){
        String q="SELECT vp.codigo_producto codigo,vp.nombre_producto producto,SUM(vp.cantidad) unidades,COALESCE(SUM(vp.subtotal),0) ingresos,COALESCE(SUM(vp.costo_unitario*vp.cantidad),0) costos,COALESCE(SUM(vp.subtotal-vp.costo_unitario*vp.cantidad),0) ganancia"+from(f)+" GROUP BY vp.codigo_producto,vp.nombre_producto ORDER BY ingresos DESC,producto LIMIT 20";
        return jdbc.query(q,params(f),(rs,n)->new ProductReportRow(rs.getString("codigo"),rs.getString("producto"),rs.getLong("unidades"),zero(rs.getBigDecimal("ingresos")),zero(rs.getBigDecimal("costos")),zero(rs.getBigDecimal("ganancia"))));
    }
    private BigDecimal zero(BigDecimal n){return n==null?BigDecimal.ZERO:n;}
}
