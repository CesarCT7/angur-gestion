# Autenticación web: variante solo administrador

Una instalación nueva ejecuta `schema.sql`. Incluye las tablas comerciales, `auth_setup`, `auth_tokens` y `solicitudes_admin`, sin cuentas iniciales. `/setup` crea el primer administrador y lo marca como principal.

Para una base existente, haz un respaldo y aplica una sola vez las migraciones aplicables que describe el [README de base de datos](README.md) sobre la base exclusiva `angur_gestion_admin`. La migración `004-solicitudes-admin.sql` conserva los datos, designa al administrador activo y verificado más antiguo como principal, crea las solicitudes y deshabilita las cuentas no administradoras. La `005-auditoria-eventos.sql` habilita la bitácora de actividad usada por Historial/Auditoría. La `006-sesiones-persistentes.sql` guarda sesiones en MySQL para que sigan disponibles entre distintas instancias de la aplicación. No apliques estas migraciones a la base de la versión original.

- Spring Security solo autentica el rol `ADMINISTRADOR`. El registro y el portal de cuentas CLIENTE no están disponibles en esta variante; los documentos anteriores conservan la referencia a sus clientes históricos.
- `auth_setup` contiene una fila bloqueada mediante `FOR UPDATE`; el alta inicial permanece excluida hasta el commit, incluso con solicitudes simultáneas.
- Un administrador autenticado solicita una cuenta desde `/usuarios/nuevo`. El enlace de aprobación aleatorio se envía al correo del principal, vence en 24 horas y se guarda únicamente como SHA-256.
- Aprobar genera una invitación aleatoria al correo solicitado. Vence en 48 horas; al usarla, la persona define su contraseña y se crea una cuenta ADMINISTRADOR verificada. Rechazar invalida el flujo.
- El primer administrador es principal. El principal puede transferir la designación a otro administrador activo y verificado; el puesto debe transferirse antes de desactivar la cuenta actual.
- Recuperar acceso vence en 24 horas para verificación y 30 minutos para restablecer contraseña. Los enlaces se consumen con POST y CSRF; cambiar contraseña revoca sesiones anteriores.

Las solicitudes y aprobaciones envían correo dentro de la transacción. Si falla SMTP, el cambio de estado se revierte. No se registran contraseñas ni enlaces en logs.

Para producción, configura HTTPS en servidor/proxy, `AUTH_BASE_URL` con HTTPS y `SESSION_COOKIE_SECURE=true`. Los enlaces usan esa URL explícita, nunca el encabezado Host. El límite de intentos vive en memoria por instancia; varias réplicas requieren un límite compartido. Estos ajustes no se verificaron en esta variante.
