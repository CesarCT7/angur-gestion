-- Respaldar antes de ejecutar una sola vez sobre angur_gestion_admin.
USE angur_gestion_admin;

CREATE TABLE auditoria_eventos (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id      BIGINT UNSIGNED NOT NULL,
    entidad_tipo    VARCHAR(30) NOT NULL,
    entidad_clave   VARCHAR(100) NOT NULL,
    accion          VARCHAR(30) NOT NULL,
    descripcion    VARCHAR(500) NOT NULL,
    creado_en       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_auditoria_fecha (creado_en, id),
    INDEX ix_auditoria_usuario_fecha (usuario_id, creado_en),
    INDEX ix_auditoria_entidad (entidad_tipo, entidad_clave, creado_en)
) ENGINE = InnoDB;
