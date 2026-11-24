-- Respaldar antes de ejecutar una sola vez en la base angur_gestion_admin.
USE angur_gestion_admin;

ALTER TABLE auth_setup
    ADD COLUMN administrador_principal_id BIGINT UNSIGNED NULL,
    ADD CONSTRAINT fk_auth_setup_principal FOREIGN KEY (administrador_principal_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT;

UPDATE auth_setup
SET administrador_principal_id = (
    SELECT id FROM usuarios
    WHERE rol = 'ADMINISTRADOR' AND activo = TRUE AND correo_verificado = TRUE
    ORDER BY id LIMIT 1
)
WHERE id = 1;

CREATE TABLE solicitudes_admin (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario VARCHAR(80) NOT NULL,
    correo VARCHAR(254) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    solicitado_por BIGINT UNSIGNED NOT NULL,
    administrador_principal_id BIGINT UNSIGNED NOT NULL,
    token_aprobacion_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    token_aprobacion_vence_en TIMESTAMP(6) NOT NULL,
    token_registro_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    token_registro_vence_en TIMESTAMP(6) NULL,
    estado ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'COMPLETADA', 'VENCIDA') NOT NULL DEFAULT 'PENDIENTE',
    cuenta_creada_id BIGINT UNSIGNED NULL,
    creado_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resuelto_en TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_solicitudes_admin_aprobacion UNIQUE (token_aprobacion_hash),
    CONSTRAINT uq_solicitudes_admin_registro UNIQUE (token_registro_hash),
    CONSTRAINT fk_solicitudes_admin_solicitante FOREIGN KEY (solicitado_por)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_solicitudes_admin_principal FOREIGN KEY (administrador_principal_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_solicitudes_admin_cuenta FOREIGN KEY (cuenta_creada_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_solicitudes_admin_estado_fecha (estado, creado_en),
    INDEX ix_solicitudes_admin_correo (correo, estado)
) ENGINE = InnoDB;

-- Cuentas que no son administradoras conservan sus datos históricos, pero no se usan
-- para iniciar sesión en esta variante de acceso administrativo.
UPDATE usuarios SET activo = FALSE WHERE rol <> 'ADMINISTRADOR';
