# 0008. Despliegue gratuito en Neon, Render y Vercel

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

La demo pública sirve para enseñar el proyecto (portfolio, entrevistas) sin que nadie tenga que instalar nada.
Tiene que costar cero, mantenerse sin intervención y no acumular lo que cambien las visitas, que comparten las
mismas cuentas de prueba.

## Decisión

| Pieza | Servicio | Por qué |
| --- | --- | --- |
| Frontend | **Vercel** | Sirve la build estática desde una CDN y redirige `/api` a la API (`vercel.json`): para el navegador todo es un solo origen, sin CORS |
| API | **Render**, con el `Dockerfile` | Plan gratuito con Docker y despliegue automático desde `master` (`render.yaml`) |
| Base de datos | **Neon** (PostgreSQL) | La base de datos gratuita de Render se borra a los 30 días; la de Neon no. Misma región que la API (Frankfurt) |

- El perfil `demo` carga datos ficticios coherentes y, con `DEMO_DAILY_RESET=true`, los vuelve a cargar desde cero
  cada día.
- En la demo, el tutorial de bienvenida sale en cada entrada y el login ofrece "Entrar como …" con un clic.
- Un flujo programado de GitHub Actions avisa a la API cada 10 minutos durante el día para que Render no la duerma.

## Alternativas consideradas

- **Un VPS (Hetzner, DigitalOcean).** Sin esperas de arranque, pero cuesta dinero cada mes y hay que mantener el
  servidor.
- **Todo en Render.** Más sencillo, pero su PostgreSQL gratuito caduca.

## Consecuencias

- Coste cero y despliegue automático: un push a `master` publica la API en Render y el frontend en Vercel.
- El plan gratuito de Render duerme la API tras 15 minutos sin tráfico, y despertarla tarda alrededor de un
  minuto. Se mitiga con el aviso programado durante el día y con un mensaje en el login cuando tarda.
- De noche se deja dormir, para quedar lejos del límite de 750 horas gratuitas al mes de Render.
- La misma imagen Docker sirve para la demo y para un centro de verdad, que solo cambiaría las variables de
  entorno ([README](../../README.md#despliegue)).
