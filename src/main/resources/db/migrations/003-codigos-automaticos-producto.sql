-- Respaldar la base y ejecutar una sola vez para habilitar códigos automáticos permanentes.
USE angur_gestion_admin;

CREATE TABLE codigos_producto (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo      VARCHAR(80) NULL,
    creado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_codigos_producto_codigo UNIQUE (codigo)
) ENGINE = InnoDB;

-- Reserva los códigos que ya existen para que el generador no los asigne.
INSERT INTO codigos_producto (codigo)
SELECT codigo
FROM productos
ORDER BY id;
