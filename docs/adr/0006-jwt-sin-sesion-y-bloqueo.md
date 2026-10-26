# 0006. Autenticación JWT sin sesión y bloqueo tras 5 intentos

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

El frontend es una aplicación React que habla con una API REST. En la demo, frontend y API están en servicios
distintos (Vercel y Render), aunque el navegador los ve en el mismo origen gracias a la redirección de `/api`. El
centro pidió además que una cuenta no se bloquee por equivocarse pocas veces: nunca con menos de 5 intentos.

## Decisión

- **Tokens JWT firmados con HS256** por la propia API (Spring Security como servidor de recursos OAuth2). Duran
  una jornada de trabajo (`PT8H`). El frontend los guarda en `sessionStorage` y los envía en la cabecera
  `Authorization` de cada petición.
- **Sin sesión en el servidor** (`STATELESS`) y, por tanto, sin CSRF: no hay cookie que un tercero pueda
  aprovechar.
- **Permisos por especialidad** en una sola clase, `SecurityConfig`, con las reglas del centro comentadas junto a
  cada ruta. Las reglas más finas (quién redacta qué apartado de un informe) están en el dominio.
- **Bloqueo por intentos**: tras 5 fallos seguidos con el mismo email, 15 minutos de bloqueo. Se cuentan en memoria
  (`InMemoryLoginAttempts`), y el propio código rechaza configurar menos de 5.

## Alternativas consideradas

- **Sesión con cookie.** Más sencilla de revocar, pero obliga a proteger contra CSRF y a compartir la sesión si un
  día hubiera más de un servidor.
- **Un proveedor de identidad externo (Keycloak, Auth0).** Más completo, pero desproporcionado para un centro y
  con coste o infraestructura extra.
- **Contar los intentos en la base de datos.** Sobreviviría a un reinicio, pero el bloqueo sirve para frenar a quien
  prueba contraseñas, no como registro, y con un solo servidor la memoria basta.

## Consecuencias

- La API no guarda estado de sesión: cualquier instancia puede atender cualquier petición.
- Un JWT no se puede revocar antes de que caduque. Para compensarlo, `CurrentUserJwtConverter` carga al empleado
  en cada petición: si lo han borrado, su token deja de servir al momento, y si le cambian la especialidad, sus
  permisos cambian en la siguiente petición. El precio es una consulta por petición, asumible con los usuarios
  de un centro.
- Al cerrar la pestaña se cierra la sesión (`sessionStorage`), algo deseable en equipos compartidos del centro.
- Si la API se reinicia, los contadores de intentos vuelven a cero.
