# gespatiens

## Descripción

**Gespatiens** es una aplicación web para la gestión de la comunidad terapéutica "Los Vientos". Su propósito es
agilizar la documentación de los residentes: el diario del centro, las actividades y sus valoraciones, las
intervenciones de los tutores y los informes de media estancia y fin de programa. La administración gestiona las
fichas de los residentes, los perfiles de los empleados y el cuadrante de turnos. Además, el personal tiene una vía
de comunicación (los avisos) y herramientas para valorar a los residentes en las actividades que organiza.

Es la reescritura del proyecto final de DAW (2023/24, originalmente en Laravel) con:

- **Backend:** Java 25 y Spring Boot 4, con arquitectura hexagonal y DDD (un módulo por contexto:
  `access`, `patients`, `diary`, `interventions`, `reports`, `notices`, `rota`, `dashboard`, `editing`).
- **Base de datos:** PostgreSQL. El esquema lo crean las migraciones de Flyway (`src/main/resources/db/migration`).
- **Frontend:** React, TypeScript y Vite con el sistema de diseño Carbon (`frontend/`, estilo en `DESIGN.md`).
- **Informes en PDF:** OpenPDF.

## Instalación

Requisitos: JDK 25, Node.js 24 y PostgreSQL.

1. Clona el repositorio.
2. Crea la base de datos vacía `gespatiens-refactor` en PostgreSQL. Al arrancar, Flyway crea las tablas.
3. Define las variables de entorno (en IntelliJ, en la configuración de ejecución):

   | Variable | Obligatoria | Para qué |
   | --- | --- | --- |
   | `DB_PASSWORD` | Sí | Contraseña de PostgreSQL. El usuario es `postgres`, o el de `DB_USER` |
   | `JWT_SECRET` | Sí | Firma las sesiones. Al menos 32 caracteres |
   | `ADMIN_EMAIL`, `ADMIN_PHONE`, `ADMIN_PASSWORD` | La primera vez | Crean el primer administrador si no existe |
   | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | No | Servidor SMTP para los correos de bienvenida |
   | `FRONTEND_URL` | No | A dónde apuntan los enlaces de los correos. Por defecto `http://localhost:5173` |

4. Arranca la API (puerto 8080):

   ```bash
   ./mvnw spring-boot:run
   ```

5. Arranca el frontend (puerto 5173). Vite reenvía `/api` a la API:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

6. Abre http://localhost:5173.

> [!TIP]
> Sin `MAIL_HOST` no se envía ningún correo: el enlace para crear la contraseña se escribe en el log de Spring
> Boot. Para ver los correos de verdad en desarrollo sirve un buzón de pruebas como
> [Mailpit](https://mailpit.axllent.org/) (`MAIL_HOST=localhost`, `MAIL_PORT=1025`, `MAIL_SMTP_AUTH=false`,
> `MAIL_STARTTLS=false`).

## Uso

1. Entra la primera vez con el administrador de `ADMIN_EMAIL`.
2. La página inicial son los avisos, visibles para todo el personal.
3. La barra lateral lleva al diario, los informes, las valoraciones, el cuadrante de turnos, las intervenciones,
   los residentes, los antiguos residentes y los empleados. La administración tiene además su panel de control.
4. Lo que cada empleado puede hacer depende de su especialidad. Los apartados que no le corresponden se ven en modo
   consulta.
5. El diario se consulta por día, y las valoraciones por mes y tipo de actividad.
6. El panel de control avisa de los informes de media estancia que tocan (a los 3 meses de la entrada) y de las
   medidas judiciales que terminan en los próximos 30 días.
7. El registro de accesos (solo administración) dice quién ha consultado o cambiado los datos de cada residente.

> [!NOTE]
> Al dar de alta a un empleado, le llega un correo con un enlace para crear su contraseña. El enlace caduca a los
> 3 días. Desde la ficha del empleado, la administración puede reenviarlo o enviar uno nuevo a quien haya olvidado
> su contraseña.

> [!TIP]
> Cada empleado cambia sus datos, su contraseña, su firma para los informes y el idioma (castellano o inglés) en
> su perfil.

> [!WARNING]
> Si un informe, un turno del diario o una actividad está abierto por otra persona, queda bloqueado hasta que lo
> cierre (RF8). El bloqueo se suelta solo al salir de la página o pasado un tiempo sin actividad.

## Tests

```bash
# Backend. Los tests de persistencia usan PostgreSQL y necesitan las variables DB_PASSWORD y JWT_SECRET
./mvnw test

# Frontend
cd frontend
npm test
npm run typecheck
```

## Licencia

Este proyecto está licenciado bajo la licencia MIT. Para más detalles, consulta el archivo [LICENSE](LICENSE).

## Autor

**Eloy Lagar Jaime**

Agradecimientos a Manuel Lagar Naharro por la idea.

## Recursos adicionales

- [Spring Boot](https://spring.io/projects/spring-boot)
- [React](https://react.dev/)
- [Carbon Design System](https://carbondesignsystem.com/)
- [PostgreSQL](https://www.postgresql.org/)
