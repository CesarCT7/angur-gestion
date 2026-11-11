# Base de datos de Angur Gestión

El esquema inicial está en [`schema.sql`](schema.sql) y requiere MySQL 8.0.16 o posterior para que se apliquen las restricciones `CHECK`.

## Creación local

1. Abre `schema.sql` en MySQL Workbench.
2. Conéctate a tu instancia local de MySQL.
3. Ejecuta el script completo una sola vez. Este crea y selecciona `angur_gestion`.
4. Confirma en el panel **SCHEMAS** que aparezcan las tablas `usuarios`, `clientes`, `productos`, `existencias`, `cotizaciones`, `cotizacion_partidas`, `ventas`, `venta_partidas` y `movimientos_inventario`.

No se agregan usuarios iniciales ni datos ficticios. Las contraseñas deben insertarse únicamente como hashes producidos por el servicio de autenticación.

## Criterios reflejados

- `usuarios.rol` contempla administrador, vendedor, cliente, supervisor de ventas y encargado de almacén.
- Productos, clientes y usuarios se desactivan con `activo`; las claves foráneas impiden borrar físicamente registros con historial.
- Cada cotización guarda el nombre, código y precio de sus partidas. Cada venta conserva además el costo unitario histórico para calcular la ganancia bruta.
- `existencias` no admite cantidades negativas. Las salidas deben bloquear la fila de existencia y actualizar saldo e historial dentro de una transacción.
- Ventas y movimientos guardan claves de operación para reconocer reintentos. El servicio debe devolver el resultado previo cuando la clave ya se procesó.
- Las claves foráneas usan `RESTRICT`: el historial no se elimina en cascada.

## Decisiones de negocio que siguen abiertas

- Los documentos requieren que una cotización inicie como `PENDIENTE`, pero el diagrama P14 propone también `BORRADOR` y `EMITIDA`. El esquema admite ambos grupos; el flujo de transición debe confirmarse con Angur Display.
- Los documentos muestran el estado `CANCELADA` para ventas, pero no definen cómo cancelar ni si se registra un movimiento compensatorio. El esquema conserva el estado; no se debe implementar la cancelación hasta acordar esa regla.
- Tipo y nombre/razón social son obligatorios para clientes; correo y teléfono se dejaron opcionales porque los campos restantes están pendientes de validación.
- Los importes usan `DECIMAL(13,2)`. La moneda no se guarda por separado porque los documentos no fijan explícitamente una política de moneda.

Los totales de encabezado deben calcularse desde las partidas en el backend. MySQL valida cada subtotal individual, pero el backend debe validar que el total de la cotización o venta coincida con la suma de sus partidas.
