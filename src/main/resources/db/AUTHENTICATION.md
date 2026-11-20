# Autenticación web

Una instalación nueva ejecuta `schema.sql`: incluye las diez tablas comerciales más `auth_setup` y `auth_tokens`, sin cuentas iniciales. Para una instalación de la versión anterior, respalda la base y ejecuta **una sola vez** `migrations/001-autenticacion-web.sql`. No vuelvas a crear todas sus tablas. La migración conserva los usuarios y habilita al administrador previo; los demás perfiles deben verificar su correo.

- `auth_setup` contiene una fila bloqueada mediante `FOR UPDATE`. El alta inicial permanece excluida hasta el commit, incluso con solicitudes simultáneas.
- Los enlaces contienen 256 bits aleatorios. Solo se almacena su SHA-256, propósito, vencimiento y fecha de uso.
- Verificar correo vence en 24 horas; restablecer contraseña, en 30 minutos. Reenviar invalida enlaces anteriores del mismo propósito. Abrir el enlace no cambia datos: hace falta POST con CSRF.
- El consumo del enlace, el cambio de contraseña y el incremento de `version_sesion` se confirman en una transacción bajo bloqueo de la cuenta. El siguiente acceso de una sesión anterior la invalida.
- El registro público crea exclusivamente `CLIENTE` y su fila de `clientes` vinculada, con el propio usuario como autor. El alta administrativa conserva al administrador como autor.
- La administración crea cuentas de los cinco perfiles y activa/desactiva accesos. No puede desactivarse a sí misma ni dejar el sistema sin administrador activo y verificado.

SMTP se utiliza dentro de la transacción de emisión. Si el envío falla, se revierte el alta/emisión. Si el envío llegó pero el commit falló, el enlace no funcionará y deberá solicitarse otro. No se registran contraseñas ni enlaces en logs.

Para producción, configura HTTPS en el servidor/proxy, `AUTH_BASE_URL` con HTTPS y `SESSION_COOKIE_SECURE=true`. Los enlaces se construyen desde esa URL explícita, nunca desde Host. El límite de intentos vive en memoria por instancia; varias réplicas necesitan un límite compartido en el proxy o servicio de acceso. Estos ajustes de producción no se han verificado en este entorno.
