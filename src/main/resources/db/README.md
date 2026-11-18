# Base de datos de Angur Gestión

El esquema inicial está en [`schema.sql`](schema.sql) y requiere MySQL 8.0.16 o posterior para que se apliquen las restricciones `CHECK`.

## Creación local

1. Abre `schema.sql` en MySQL Workbench.
2. Conéctate a tu instancia local de MySQL.
3. Ejecuta el script completo una sola vez. Este crea y selecciona `angur_gestion`.
4. Confirma en el panel **SCHEMAS** que aparezcan las tablas `usuarios`, `clientes`, `productos`, `codigos_producto`, `existencias`, `cotizaciones`, `cotizacion_partidas`, `ventas`, `venta_partidas` y `movimientos_inventario`.

No se agregan usuarios iniciales ni datos ficticios. Las contraseñas deben insertarse únicamente como hashes producidos por el servicio de autenticación.

## Criterios reflejados

- `usuarios.rol` contempla administrador, vendedor, cliente, supervisor de ventas y encargado de almacén.
- Clientes y usuarios conservan desactivación lógica. Los productos sin historial se borran físicamente; los productos con historial se archivan, pueden restaurarse y también eliminarse definitivamente.
- `codigos_producto` conserva permanentemente los códigos históricos. El sistema reserva un identificador secuencial y genera códigos `ANG-########`; nunca los modifica ni los reutiliza al borrar el producto.
- Cada cotización guarda el nombre, código y precio de sus partidas. Cada venta conserva además el costo unitario histórico para calcular la ganancia bruta.
- `existencias` no admite cantidades negativas. El servicio de inventario bloquea la fila de existencia y actualiza saldo e historial dentro de una transacción.
- Los movimientos usan una clave de operación para reconocer reintentos y devolver el resultado anterior sin volver a cambiar el saldo.
- Las partidas de ventas y cotizaciones conservan instantáneas de código, nombre, precio y costo. Al eliminar definitivamente un producto, su referencia queda en `NULL` y la instantánea permanece.
- Los movimientos guardan instantáneas de código y nombre; eliminar un producto no borra los movimientos.
- Para bases existentes, respalda y ejecuta una sola vez cada migración pendiente en orden: [`migrations/002-eliminacion-productos.sql`](migrations/002-eliminacion-productos.sql) y [`migrations/003-codigos-automaticos-producto.sql`](migrations/003-codigos-automaticos-producto.sql).

La base local `angur_gestion` usada por el paquete ejecutable ya tiene aplicadas las migraciones `002` y `003`; no las vuelvas a ejecutar ahí.

## Decisiones de negocio que siguen abiertas

- La implementación inicia cotizaciones en `PENDIENTE` y permite a administrador/vendedor cambiarlas a `ACEPTADA` o `RECHAZADA`. El diagrama P14 propone también `BORRADOR`, `EMITIDA`, `CANCELADA` y `CONVERTIDA_EN_VENTA`; estas transiciones aún deben confirmarse. Al registrar una venta desde una cotización aceptada, se conserva `ACEPTADA` y se asocia el folio de venta; no se cambia automáticamente a `CONVERTIDA_EN_VENTA` hasta acordar esa semántica.
- Los documentos muestran el estado `CANCELADA` para ventas, pero no definen cómo cancelar ni si se registra un movimiento compensatorio. El esquema conserva el estado; no se debe implementar la cancelación hasta acordar esa regla.
- Tipo y nombre/razón social son obligatorios para clientes; correo y teléfono se dejaron opcionales porque los campos restantes están pendientes de validación.
- Los importes usan `DECIMAL(13,2)`. La moneda no se guarda por separado porque los documentos no fijan explícitamente una política de moneda.

Los totales de encabezado deben calcularse desde las partidas en el backend. MySQL valida cada subtotal individual, pero el backend debe validar que el total de la cotización o venta coincida con la suma de sus partidas.
