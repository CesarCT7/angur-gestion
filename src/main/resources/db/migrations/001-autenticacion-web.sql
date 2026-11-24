-- Respaldar y ejecutar UNA vez sobre la versión anterior.
USE angur_gestion_admin;
ALTER TABLE usuarios ADD COLUMN correo_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN version_sesion BIGINT NOT NULL DEFAULT 0;
UPDATE usuarios SET correo_verificado = TRUE WHERE rol = 'ADMINISTRADOR';

-- Una fila bloqueada hasta el COMMIT evita dos administradores iniciales.
CREATE TABLE auth_setup (
    id TINYINT NOT NULL PRIMARY KEY,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_auth_setup_id CHECK (id = 1)
) ENGINE = InnoDB;
INSERT INTO auth_setup (id, completado) SELECT 1, EXISTS(SELECT 1 FROM usuarios);
CREATE TABLE auth_tokens (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    proposito ENUM('VERIFICAR_CORREO', 'RESTABLECER_CLAVE') NOT NULL,
    vence_en TIMESTAMP(6) NOT NULL,
    usado_en TIMESTAMP(6) NULL,
    creado_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_auth_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_tokens_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT,
    INDEX ix_auth_tokens_usuario (usuario_id, proposito, usado_en)
) ENGINE = InnoDB;
