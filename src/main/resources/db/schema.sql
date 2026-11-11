-- Angur Gestion - esquema inicial MySQL 8
-- Ejecutar una sola vez en MySQL Workbench o con el cliente mysql.
-- Las operaciones de negocio deben ejecutarse desde el backend dentro de transacciones.

CREATE DATABASE IF NOT EXISTS angur_gestion
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE angur_gestion;

CREATE TABLE usuarios (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario         VARCHAR(80) NOT NULL,
    correo          VARCHAR(254) NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    clave_hash      VARCHAR(255) NOT NULL COMMENT 'Verificador de contrasena; nunca texto plano',
    rol             ENUM(
                        'ADMINISTRADOR',
                        'VENDEDOR',
                        'CLIENTE',
                        'SUPERVISOR_VENTAS',
                        'ENCARGADO_ALMACEN'
                    ) NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_usuario UNIQUE (usuario),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    INDEX ix_usuarios_rol_activo (rol, activo)
) ENGINE = InnoDB;

CREATE TABLE clientes (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT UNSIGNED NULL COMMENT 'Cuenta de acceso opcional; un cliente solo puede tener una cuenta',
    creado_por          BIGINT UNSIGNED NOT NULL,
    tipo                ENUM('PARTICULAR', 'EMPRESA') NOT NULL,
    nombre_razon_social VARCHAR(180) NOT NULL,
    correo              VARCHAR(254) NULL,
    telefono            VARCHAR(30) NULL,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_clientes_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_clientes_creado_por FOREIGN KEY (creado_por)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_clientes_nombre (nombre_razon_social),
    INDEX ix_clientes_activo_tipo (activo, tipo)
) ENGINE = InnoDB;

CREATE TABLE productos (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo              VARCHAR(80) NOT NULL,
    nombre              VARCHAR(160) NOT NULL,
    descripcion         TEXT NULL,
    costo_actual        DECIMAL(13, 2) NOT NULL DEFAULT 0.00,
    precio_venta_actual DECIMAL(13, 2) NOT NULL DEFAULT 0.00,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_productos_codigo UNIQUE (codigo),
    CONSTRAINT chk_productos_costo CHECK (costo_actual >= 0),
    CONSTRAINT chk_productos_precio CHECK (precio_venta_actual >= 0),
    INDEX ix_productos_nombre (nombre),
    INDEX ix_productos_activo (activo)
) ENGINE = InnoDB;

CREATE TABLE existencias (
    producto_id     BIGINT UNSIGNED NOT NULL,
    cantidad        INT UNSIGNED NOT NULL DEFAULT 0,
    actualizado_en  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (producto_id),
    CONSTRAINT fk_existencias_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_existencias_no_negativas CHECK (cantidad >= 0)
) ENGINE = InnoDB;

CREATE TABLE cotizaciones (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cliente_id      BIGINT UNSIGNED NOT NULL,
    vendedor_id     BIGINT UNSIGNED NOT NULL,
    estado          ENUM(
                        'BORRADOR',
                        'PENDIENTE',
                        'EMITIDA',
                        'ACEPTADA',
                        'RECHAZADA',
                        'CANCELADA',
                        'CONVERTIDA_EN_VENTA'
                    ) NOT NULL DEFAULT 'PENDIENTE',
    observaciones   VARCHAR(1000) NULL,
    emitida_en      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total           DECIMAL(13, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    CONSTRAINT fk_cotizaciones_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_cotizaciones_vendedor FOREIGN KEY (vendedor_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_cotizaciones_total CHECK (total >= 0),
    INDEX ix_cotizaciones_cliente_fecha (cliente_id, emitida_en),
    INDEX ix_cotizaciones_vendedor_fecha (vendedor_id, emitida_en),
    INDEX ix_cotizaciones_estado_fecha (estado, emitida_en)
) ENGINE = InnoDB;

CREATE TABLE cotizacion_partidas (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cotizacion_id       BIGINT UNSIGNED NOT NULL,
    producto_id         BIGINT UNSIGNED NOT NULL,
    codigo_producto     VARCHAR(80) NOT NULL COMMENT 'Instantanea para conservar el detalle historico',
    nombre_producto     VARCHAR(160) NOT NULL COMMENT 'Instantanea para conservar el detalle historico',
    cantidad            INT UNSIGNED NOT NULL,
    precio_unitario     DECIMAL(13, 2) NOT NULL,
    subtotal            DECIMAL(13, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_cotizacion_producto UNIQUE (cotizacion_id, producto_id),
    CONSTRAINT fk_cotizacion_partidas_cotizacion FOREIGN KEY (cotizacion_id)
        REFERENCES cotizaciones (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_cotizacion_partidas_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_cotizacion_partida_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_cotizacion_partida_precio CHECK (precio_unitario >= 0),
    CONSTRAINT chk_cotizacion_partida_subtotal CHECK (subtotal = ROUND(cantidad * precio_unitario, 2)),
    INDEX ix_cotizacion_partidas_producto (producto_id)
) ENGINE = InnoDB;

CREATE TABLE ventas (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cliente_id          BIGINT UNSIGNED NULL COMMENT 'NULL permite ventas directas sin cliente registrado',
    cotizacion_id       BIGINT UNSIGNED NULL,
    vendedor_id         BIGINT UNSIGNED NOT NULL,
    clave_operacion     VARCHAR(100) NOT NULL COMMENT 'Clave de idempotencia compartida por reintentos',
    estado              ENUM('REGISTRADA', 'CANCELADA') NOT NULL DEFAULT 'REGISTRADA',
    fecha               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total               DECIMAL(13, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    CONSTRAINT uq_ventas_clave_operacion UNIQUE (clave_operacion),
    CONSTRAINT uq_ventas_cotizacion UNIQUE (cotizacion_id),
    CONSTRAINT fk_ventas_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_ventas_cotizacion FOREIGN KEY (cotizacion_id)
        REFERENCES cotizaciones (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_ventas_vendedor FOREIGN KEY (vendedor_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_ventas_total CHECK (total >= 0),
    INDEX ix_ventas_cliente_fecha (cliente_id, fecha),
    INDEX ix_ventas_vendedor_fecha (vendedor_id, fecha),
    INDEX ix_ventas_estado_fecha (estado, fecha)
) ENGINE = InnoDB;

CREATE TABLE venta_partidas (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id            BIGINT UNSIGNED NOT NULL,
    producto_id         BIGINT UNSIGNED NOT NULL,
    codigo_producto     VARCHAR(80) NOT NULL COMMENT 'Instantanea para conservar el detalle historico',
    nombre_producto     VARCHAR(160) NOT NULL COMMENT 'Instantanea para conservar el detalle historico',
    cantidad            INT UNSIGNED NOT NULL,
    precio_unitario     DECIMAL(13, 2) NOT NULL,
    costo_unitario      DECIMAL(13, 2) NOT NULL,
    subtotal            DECIMAL(13, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_venta_producto UNIQUE (venta_id, producto_id),
    CONSTRAINT fk_venta_partidas_venta FOREIGN KEY (venta_id)
        REFERENCES ventas (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_venta_partidas_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_venta_partida_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_venta_partida_precio CHECK (precio_unitario >= 0),
    CONSTRAINT chk_venta_partida_costo CHECK (costo_unitario >= 0),
    CONSTRAINT chk_venta_partida_subtotal CHECK (subtotal = ROUND(cantidad * precio_unitario, 2)),
    INDEX ix_venta_partidas_producto (producto_id)
) ENGINE = InnoDB;

CREATE TABLE movimientos_inventario (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    producto_id             BIGINT UNSIGNED NOT NULL,
    usuario_id              BIGINT UNSIGNED NOT NULL,
    venta_id                BIGINT UNSIGNED NULL COMMENT 'Se llena para las salidas generadas por ventas',
    tipo                    ENUM('ENTRADA', 'SALIDA') NOT NULL,
    cantidad                INT UNSIGNED NOT NULL,
    existencia_resultante   INT UNSIGNED NOT NULL,
    motivo                  VARCHAR(255) NOT NULL,
    clave_operacion         VARCHAR(100) NOT NULL COMMENT 'Evita duplicar entradas, salidas o ventas reintentadas',
    fecha                   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_movimientos_operacion_producto_tipo UNIQUE (clave_operacion, producto_id, tipo),
    CONSTRAINT fk_movimientos_producto FOREIGN KEY (producto_id)
        REFERENCES productos (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_movimientos_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_movimientos_venta FOREIGN KEY (venta_id)
        REFERENCES ventas (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_movimientos_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_movimientos_existencia CHECK (existencia_resultante >= 0),
    INDEX ix_movimientos_producto_fecha (producto_id, fecha),
    INDEX ix_movimientos_usuario_fecha (usuario_id, fecha),
    INDEX ix_movimientos_venta (venta_id)
) ENGINE = InnoDB;
