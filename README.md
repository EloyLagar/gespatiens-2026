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

## Probarla con Docker

Con [Docker Desktop](https://www.docker.com/products/docker-desktop/) basta con un comando. Arranca PostgreSQL, la
API y el frontend, y carga los datos de demostración la primera vez:

```bash
docker compose up --build
```

Abre http://localhost:8080 y entra con `marta.serra@vientos.es` (administración) o con cualquier otro empleado de
la demostración (`ana.beltran@`, `carmen.lopez@`, `laura.gil@`, `jorge.vidal@`…, todos `@vientos.es`). La contraseña
de todos es `demo1234`.

Los datos son inventados: diez residentes en el centro y tres antiguos, un mes de diario con valoraciones,
intervenciones, informes en distintas fases, avisos y el cuadrante de dos meses. Las fechas se cuentan desde el día en
que se cargan. Para cambiar el puerto, las contraseñas o empezar con la base de datos limpia, copia `.env.example`
como `.env`. `docker compose down -v` lo borra todo.

> [!WARNING]
> Los valores por defecto (`JWT_SECRET`, contraseña de la base de datos) solo valen para probar en el propio equipo.
> Si la app va a estar en internet, cámbialos en el `.env` y usa HTTPS.

## Publicarla gratis

Tres servicios con plan gratuito, cada uno con su parte:

| Parte | Servicio | Configuración |
|---|---|---|
| Base de datos | [Neon](https://neon.tech) (PostgreSQL) | Ninguna: Flyway crea las tablas al arrancar la API |
| API | [Render](https://render.com) | `render.yaml` |
| Frontend | [Vercel](https://vercel.com) | `frontend/vercel.json` |

1. **Neon**: crea un proyecto en la región *AWS Europe Central 1 (Frankfurt)*, la misma que usa la API en Render, con una base de datos `gespatiens` y copia la cadena de conexión *directa* (la que no
   lleva `-pooler` en el host; Flyway no se lleva bien con el pooler).
2. **Render**: *New → Blueprint* sobre este repositorio. Pide `DB_URL`
   (`jdbc:postgresql://<host>/gespatiens?sslmode=require`), `DB_USER`, `DB_PASSWORD` y `FRONTEND_URL` (la dirección
   que dé Vercel en el paso siguiente; se puede rellenar después). `JWT_SECRET` lo genera Render.
3. **Vercel**: *Add New → Project* sobre este repositorio con *Root Directory* `frontend`. Detecta Vite solo.
   Si el servicio de Render no se llama `gespatiens-api`, cambia la dirección en `frontend/vercel.json`.

Vercel reenvía `/api` a Render, así que la app sigue en un solo origen y no hace falta CORS.

Cualquiera puede entrar con las cuentas de la demostración, así que en Render la demo se vuelve a cargar desde cero
cada día (`DEMO_DAILY_RESET=true`): a las 4:00 o, si la API estaba dormida a esa hora, al despertar.

> [!NOTE]
> La API gratuita de Render se duerme tras 15 minutos sin visitas y tarda alrededor de un minuto en despertar: el
> primer inicio de sesión puede ir lento.

## Instalación para desarrollo

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
6. El panel de control avisa de los informes de media estancia que tocan (a mitad del programa: por defecto a los
   3 meses de la entrada, y la fecha se cambia en la cabecera de cada informe) y de las
   medidas judiciales que terminan en los próximos 30 días.
7. El registro de cambios (solo administración) dice quién ha cambiado los datos de cada residente; se conserva 2 años.

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
# Backend. Los que usan PostgreSQL necesitan DB_PASSWORD y JWT_SECRET, y trabajan en su propio esquema
# (gespatiens_test, que Flyway crea solo): no tocan los datos de la app
./mvnw test

# Frontend
cd frontend
npm test
npm run typecheck

# De extremo a extremo (Playwright), contra la app de docker compose con los datos de demostración
cd e2e
npm install
npx playwright install chromium
npx playwright test
```

La integración continua (`.github/workflows/ci.yml`) lo pasa todo en cada push: los tests del backend contra un
PostgreSQL de usar y tirar, los del frontend con su build, y las pruebas de extremo a extremo sobre la app
arrancada con `docker compose`.

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
