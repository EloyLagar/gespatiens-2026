# gespatiens · frontend

Aplicación React + TypeScript (Vite) con los componentes de [Carbon](https://carbondesignsystem.com/).
El estilo y las reglas de accesibilidad están en el `DESIGN.md` de la raíz del repositorio.

## Arrancar en desarrollo

1. Arranca la API de Spring Boot (puerto 8080) con las variables `DB_PASSWORD`, `JWT_SECRET`, etc.
2. En esta carpeta:

```bash
npm install
npm run dev
```

3. Abre http://localhost:5173 e inicia sesión con un empleado.

Vite reenvía las peticiones a `/api` hacia `http://localhost:8080` (`vite.config.ts`), así que para el
navegador todo es el mismo origen y la API no necesita CORS.

## Comandos

| Comando | Qué hace |
| --- | --- |
| `npm run dev` | Servidor de desarrollo con recarga en caliente |
| `npm test` | Tests con Vitest |
| `npm run typecheck` | Comprobación de tipos |
| `npm run build` | Build de producción en `dist/` |

## Estructura

- `src/api/` — el único sitio que habla con la API (`fetch`, token, errores ProblemDetail).
- `src/auth/` — sesión (token JWT en `sessionStorage`) y rutas protegidas.
- `src/features/` — pantallas por módulo, igual que en el backend: `patients/`, `users/`, `auth/`.
- `src/i18n/` — textos en castellano e inglés (RNF9). Ningún texto va escrito en los componentes.
- `src/shared/` y `src/layout/` — componentes comunes y el marco de la aplicación.
