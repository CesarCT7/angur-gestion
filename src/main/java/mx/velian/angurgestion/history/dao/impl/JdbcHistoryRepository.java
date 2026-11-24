package mx.velian.angurgestion.history.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import mx.velian.angurgestion.history.dao.HistoryRepository;
import mx.velian.angurgestion.history.model.HistoryEntry;
import mx.velian.angurgestion.history.model.HistoryFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcHistoryRepository implements HistoryRepository {
    private static final String UNION = """
            SELECT 'COTIZACION' tipo, c.id, c.emitida_en fecha, c.estado, u.nombre actor,
                   COALESCE(cl.nombre_razon_social,'Sin cliente') cliente, COALESCE(p.productos,'') producto,
                   CONCAT('Cotización #',c.id,CASE WHEN c.observaciones IS NULL OR c.observaciones='' THEN '' ELSE CONCAT(' · ',c.observaciones) END) descripcion,
                   c.total monto, CONCAT('/cotizaciones/',c.id) enlace
            FROM cotizaciones c JOIN usuarios u ON u.id=c.vendedor_id
            LEFT JOIN clientes cl ON cl.id=c.cliente_id
            LEFT JOIN (SELECT cotizacion_id,GROUP_CONCAT(DISTINCT nombre_producto ORDER BY nombre_producto SEPARATOR ', ') productos FROM cotizacion_partidas GROUP BY cotizacion_id) p ON p.cotizacion_id=c.id
            UNION ALL
            SELECT 'VENTA', v.id, v.fecha, v.estado, u.nombre,
                   COALESCE(cl.nombre_razon_social,'Sin cliente'), COALESCE(p.productos,''),
                   CONCAT('Venta #',v.id,CASE WHEN v.cotizacion_id IS NULL THEN '' ELSE CONCAT(' · Cotización #',v.cotizacion_id) END),
                   v.total, CONCAT('/ventas/',v.id)
            FROM ventas v JOIN usuarios u ON u.id=v.vendedor_id
            LEFT JOIN clientes cl ON cl.id=v.cliente_id
            LEFT JOIN (SELECT venta_id,GROUP_CONCAT(DISTINCT nombre_producto ORDER BY nombre_producto SEPARATOR ', ') productos FROM venta_partidas GROUP BY venta_id) p ON p.venta_id=v.id
            UNION ALL
            SELECT 'INVENTARIO', m.id, m.fecha, m.tipo, u.nombre, '', m.nombre_producto,
                   CONCAT(m.tipo,' de ',m.cantidad,' unidades · ',m.motivo,' · Existencia final ',m.existencia_resultante), 0.00, '/inventario'
            FROM movimientos_inventario m JOIN usuarios u ON u.id=m.usuario_id
            UNION ALL
            SELECT 'AUDITORIA', a.id, a.creado_en, a.accion, u.nombre, '', a.entidad_clave,
                   CONCAT(a.entidad_tipo,' · ',a.descripcion), 0.00, ''
            FROM auditoria_eventos a JOIN usuarios u ON u.id=a.usuario_id
            """;

    private final JdbcTemplate jdbc;
    public JdbcHistoryRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Override
    public List<HistoryEntry> find(HistoryFilter f) {
        StringBuilder sql=new StringBuilder("SELECT * FROM (").append(UNION).append(") h WHERE h.fecha>=? AND h.fecha<?");
        List<Object> args=new ArrayList<>();
        args.add(Timestamp.valueOf(f.from().atStartOfDay()));
        args.add(Timestamp.valueOf(f.to().plusDays(1).atStartOfDay()));
        add(sql,args,"h.tipo",f.type(),false);
        add(sql,args,"h.estado",f.status(),false);
        add(sql,args,"h.producto",f.product(),true);
        add(sql,args,"h.actor",f.actor(),true);
        if(f.search()!=null&&!f.search().isBlank()){
            sql.append(" AND CONCAT(h.tipo,' ',h.id,' ',h.estado,' ',h.actor,' ',h.producto,' ',h.descripcion) LIKE ?");
            args.add("%"+f.search().trim()+"%");
        }
        sql.append(" ORDER BY h.fecha DESC,h.id DESC LIMIT 500");
        return jdbc.query(sql.toString(),(rs,n)->new HistoryEntry(rs.getString("tipo"),rs.getLong("id"),rs.getTimestamp("fecha").toLocalDateTime(),rs.getString("estado"),rs.getString("actor"),rs.getString("cliente"),rs.getString("producto"),rs.getString("descripcion"),rs.getBigDecimal("monto"),rs.getString("enlace")),args.toArray());
    }

    private void add(StringBuilder sql,List<Object> args,String column,String value,boolean contains){
        if(value==null||value.isBlank())return;
        sql.append(" AND ").append(column).append(contains?" LIKE ?":"=?");
        args.add(contains?"%"+value.trim()+"%":value.trim());
    }
}
