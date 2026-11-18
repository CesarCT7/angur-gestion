-- Respaldar la base y ejecutar una sola vez para habilitar el borrado definitivo
-- de productos archivados sin perder partidas ni movimientos historicos.
USE angur_gestion;

ALTER TABLE movimientos_inventario
    ADD COLUMN codigo_producto VARCHAR(80) NULL AFTER producto_id,
    ADD COLUMN nombre_producto VARCHAR(160) NULL AFTER codigo_producto;

UPDATE movimientos_inventario m
JOIN productos p ON p.id = m.producto_id
SET m.codigo_producto = p.codigo,
    m.nombre_producto = p.nombre;

ALTER TABLE movimientos_inventario
    DROP FOREIGN KEY fk_movimientos_producto;

ALTER TABLE movimientos_inventario
    MODIFY COLUMN producto_id BIGINT UNSIGNED NULL,
    MODIFY COLUMN codigo_producto VARCHAR(80) NOT NULL,
    MODIFY COLUMN nombre_producto VARCHAR(160) NOT NULL;

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT fk_movimientos_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON UPDATE RESTRICT ON DELETE SET NULL;

ALTER TABLE cotizacion_partidas
    DROP FOREIGN KEY fk_cotizacion_partidas_producto;

ALTER TABLE cotizacion_partidas
    MODIFY COLUMN producto_id BIGINT UNSIGNED NULL COMMENT 'NULL si el producto se elimino; codigo y nombre conservan la instantanea';

ALTER TABLE cotizacion_partidas
    ADD CONSTRAINT fk_cotizacion_partidas_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON UPDATE RESTRICT ON DELETE SET NULL;

ALTER TABLE venta_partidas
    DROP FOREIGN KEY fk_venta_partidas_producto;

ALTER TABLE venta_partidas
    MODIFY COLUMN producto_id BIGINT UNSIGNED NULL COMMENT 'NULL si el producto se elimino; codigo y nombre conservan la instantanea';

ALTER TABLE venta_partidas
    ADD CONSTRAINT fk_venta_partidas_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON UPDATE RESTRICT ON DELETE SET NULL;
