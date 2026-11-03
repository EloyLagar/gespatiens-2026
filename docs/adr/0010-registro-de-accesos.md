# 0010. Registro de accesos separado del registro de cambios

**Estado:** aceptada · **Fecha:** 2026-11

## Contexto

gespatiens se instalará en el servidor interno de la empresa que gestiona el centro. Como da un servicio público,
le aplica el Esquema Nacional de Seguridad (previsiblemente en categoría MEDIA), que pide registrar la actividad de
los usuarios (op.exp.8) y en particular los accesos al sistema (op.acc). El registro de cambios (`audit_log`) ya
contaba quién tocaba los datos de cada residente, pero no quién entraba en la aplicación ni quién lo intentaba sin
conseguirlo. El bloqueo tras 5 fallos (ADR 0006) frena a quien prueba contraseñas, pero vive en memoria y no deja
rastro.

## Decisión

- **Una tabla propia, `access_log`**, en el módulo `audit`: cada intento de entrar, con el momento, el email que se
  escribió (exista o no), el empleado de ese email si existe, el resultado, la IP y el navegador.
- **Cuatro resultados**: `SUCCESS` (entró), `FAILURE` (email o contraseña incorrectos), `LOCKED` (el fallo que
  agotó los intentos y bloqueó el email) y `BLOCKED` (lo intentó con el email ya bloqueado). Así, filtrar por
  `LOCKED` responde a «¿alguien ha estado probando contraseñas?».
- **El login avisa con un evento** (ADR 0009): `LoginUseCase` llama al puerto `LoginEvents`, `SpringLoginEvents` lo
  publica y `LoginAccessListener` lo anota. El módulo `access` no sabe que existe el registro.
- **Si anotar falla, el login sigue**: el listener lo deja en el log de la aplicación. Que el registro no responda
  no debe dejar al personal sin entrar.
- **La IP es la del cliente** aunque haya un proxy delante (`server.forward-headers-strategy=native`): Tomcat se fía
  de `X-Forwarded-For` solo si llega desde una dirección privada, así que desde fuera no se puede falsear.
- **En la demo publicada la IP se guarda sin su última parte** (`gespatiens.access-log.anonymize-ip`): sirve para
  enseñar la pantalla sin guardar la dirección exacta de cada visitante.
- **Se conserva 2 años**, como el registro de cambios, y lo borra el mismo trabajo nocturno (`AuditPurgeJob`).
- **Solo la administración lo consulta**, en la pestaña «Accesos» de Registros (`/registro/accesos`).

## Alternativas consideradas

- **Añadir acciones `LOGIN_*` al registro de cambios.** Una sola tabla, pero sus columnas son de residentes
  (`patient_id`, `resource_id`) y le faltan las que importan aquí (email, IP, navegador). Mezclarlas obligaría a
  dejar casi todo a null y a filtrar por tipo en cada consulta.
- **Solo el log de la aplicación (ficheros).** No hace falta tabla, pero no se puede consultar desde la aplicación,
  y los ficheros se rotan sin control de cuánto se conservan.
- **Guardar también el cierre de sesión.** Con JWT sin sesión (ADR 0006), cerrar la pestaña cierra la sesión sin
  avisar al servidor: el registro solo podría tener los cierres voluntarios, y daría una imagen incompleta.

## Consecuencias

- Cada login hace una escritura más en la base de datos. Con el personal de un centro, no se nota.
- El email que se escribe en un intento fallido puede no ser de nadie (o ser una contraseña escrita en el campo
  equivocado). Se guarda recortado a 254 caracteres y solo lo ve la administración.
- La IP y el navegador son datos personales del empleado: entran en el registro de actividades de tratamiento del
  centro, con la misma finalidad (seguridad) y el mismo plazo que el registro de cambios.
- Los contadores del bloqueo siguen en memoria: el registro deja constancia, pero no cambia cómo se bloquea.
