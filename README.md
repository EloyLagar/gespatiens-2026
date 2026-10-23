# Gespatiens

[![CI](https://github.com/EloyLagar/gespatiens-hexagonal/actions/workflows/ci.yml/badge.svg)](https://github.com/EloyLagar/gespatiens-hexagonal/actions/workflows/ci.yml)
[![Licencia MIT](https://img.shields.io/badge/licencia-MIT-blue.svg)](LICENSE)
![Java 25](https://img.shields.io/badge/Java-25-orange)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4-6DB33F)
![React](https://img.shields.io/badge/React-TypeScript-61DAFB)

Aplicación web para la gestión de una comunidad terapéutica de tratamiento de adicciones. Reúne en un solo sitio la
documentación diaria de los residentes: el diario del centro, las actividades y sus valoraciones, las intervenciones
de los tutores, los informes de media estancia y fin de programa, el cuadrante de turnos y los avisos del personal.

Es la reescritura completa del proyecto final del ciclo de Desarrollo de Aplicaciones Web (IES Serpis, 2023/24),
desarrollado originalmente en Laravel, con una arquitectura hexagonal y diseño guiado por el dominio.

## Demo

| Perfil | Email | Contraseña |
| --- | --- | --- |
| Administración | `marta.serra@vientos.es` | `demo1234` |
| Educación social | `ana.beltran@vientos.es` | `demo1234` |
| Psicología | `jorge.vidal@vientos.es` | `demo1234` |
| Equipo médico | `laura.gil@vientos.es` | `demo1234` |
| Trabajo social | `carmen.lopez@vientos.es` | `demo1234` |

Todos los datos son ficticios: diez residentes en el centro y tres antiguos, un mes de diario con valoraciones,
intervenciones, informes en distintas fases, avisos y dos meses de cuadrante. La demo pública se restablece cada día,
y el primer acceso puede tardar alrededor de un minuto mientras arranca el servidor.

## Funcionalidades

- **Diario del centro**: turnos de mañana, tarde y noche, actividades programadas por día con su asistencia, salidas
  terapéuticas y reducciones de actividad.
- **Valoraciones**: notas de 0 a 10 por actividad y una vista mensual por tipo de actividad.
- **Intervenciones**: cada residente tiene dos tutores (educador y psicólogo), que son quienes registran sus
  intervenciones.
- **Informes**: el informe de media estancia y el de fin de programa se crean solos con cada estancia. Cada área
  redacta sus apartados, y el informe se firma y se exporta a PDF.
- **Residentes**: ingreso, reingreso, alta, historial de estancias, derivaciones y visitas autorizadas.
- **Cuadrante de turnos**: planificación del personal y cambios de turno entre compañeros.
- **Panel de control**: vencimientos cercanos de informes y de medidas judiciales.
- **Avisos**: canal de comunicación interno para todo el personal.
- **Permisos por especialidad**: cada empleado edita lo que le corresponde y consulta el resto.
- **Edición concurrente segura**: un documento abierto por otra persona queda bloqueado hasta que lo libera.
- **Registro de cambios**: quién modificó los datos de cada residente y cuándo, durante dos años.
- **Interfaz accesible (WCAG AA) y bilingüe** (castellano e inglés).

## Arquitectura

El backend está organizado en módulos por contexto delimitado: `access`, `patients`, `diary`, `interventions`,
`reports`, `rota`, `notices`, `dashboard`, `editing` y `audit`. Cada uno sigue la arquitectura hexagonal:

```
<módulo>/
├── domain/          Agregados, objetos de valor y puertos (interfaces de repositorio). Sin dependencias de Spring
├── application/     Casos de uso
└── infrastructure/  Adaptadores: controladores REST, persistencia JPA, seguridad y configuración
```

Los agregados se crean mediante métodos de fábrica que validan sus invariantes, y los identificadores se generan en
el dominio. El esquema de la base de datos se versiona con migraciones de Flyway.

## Tecnologías

| Capa | Tecnologías |
| --- | --- |
| Backend | Java 25, Spring Boot 4, Spring Security (JWT), Spring Data JPA |
| Base de datos | PostgreSQL, Flyway |
| Frontend | React, TypeScript, Vite, Carbon Design System |
| Informes | OpenPDF |
| Pruebas | JUnit, Vitest, Playwright |
| Infraestructura | Docker, GitHub Actions, Render, Vercel, Neon |

## Ejecución con Docker

Requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
docker compose up --build
```

Este comando arranca PostgreSQL, la API y el frontend, y carga los datos de demostración. La aplicación queda
disponible en http://localhost:8080 con las cuentas de la [demo](#demo).

El puerto, las contraseñas y el perfil se configuran copiando `.env.example` como `.env`. `docker compose down -v`
elimina los contenedores y los datos.

> [!WARNING]
> Los valores por defecto de `JWT_SECRET` y de la contraseña de la base de datos solo sirven para uso local.

## Desarrollo

Requisitos: JDK 25, Node.js 24 y PostgreSQL.

1. Crea una base de datos vacía `gespatiens-refactor`. Flyway crea las tablas al arrancar.
2. Define las variables de entorno:

   | Variable | Obligatoria | Descripción |
   | --- | --- | --- |
   | `DB_PASSWORD` | Sí | Contraseña de PostgreSQL (usuario `postgres` o el indicado en `DB_USER`) |
   | `JWT_SECRET` | Sí | Clave de firma de las sesiones, de al menos 32 caracteres |
   | `ADMIN_EMAIL`, `ADMIN_PHONE`, `ADMIN_PASSWORD` | Primer arranque | Crean el primer administrador |
   | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | No | Servidor SMTP para los correos de alta |
   | `FRONTEND_URL` | No | Destino de los enlaces de los correos (por defecto `http://localhost:5173`) |

3. Arranca la API en el puerto 8080:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Arranca el frontend en el puerto 5173. Vite redirige `/api` a la API:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

Sin `MAIL_HOST` no se envían correos: el enlace de creación de contraseña de los nuevos empleados se escribe en el
log. Para probar los correos en local se puede usar [Mailpit](https://mailpit.axllent.org/).

## Pruebas

```bash
# Backend (las pruebas de persistencia usan PostgreSQL en un esquema propio, gespatiens_test)
./mvnw test

# Frontend
cd frontend
npm test
npm run typecheck

# Extremo a extremo, contra la aplicación levantada con docker compose
cd e2e
npm install
npx playwright install chromium
npx playwright test
```

La integración continua ejecuta las tres suites en cada push: el backend contra un PostgreSQL efímero, el frontend
con su build de producción y las pruebas de extremo a extremo sobre la aplicación completa en Docker.

## Despliegue

| Componente | Servicio | Configuración |
| --- | --- | --- |
| Frontend | Vercel | `frontend/vercel.json` |
| API | Render (Docker) | `render.yaml` |
| Base de datos | Neon (PostgreSQL) | Migraciones de Flyway |

Vercel sirve el frontend y redirige `/api` a la API, de modo que la aplicación funciona en un único origen, sin
CORS. La API y la base de datos están en la misma región (Frankfurt). Con `DEMO_DAILY_RESET=true`, los datos de
demostración se restablecen cada día.

## Licencia

Distribuido bajo la licencia MIT. Consulta [LICENSE](LICENSE).

## Autor

**Eloy Lagar Jaime** · [GitHub](https://github.com/EloyLagar)

Gracias a Manuel Lagar Naharro por la idea original.
