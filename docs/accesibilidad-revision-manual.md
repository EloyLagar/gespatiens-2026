# Revisión manual de accesibilidad

axe revisa en cada cambio lo que se puede comprobar solo (contraste, nombres de botones y campos, ARIA,
tamaños), pero se le escapan cosas que solo ve una persona: el orden en que se recorre la pantalla, si un
diálogo atrapa el foco o cómo suena con un lector de pantalla. Esta lista es esa revisión. Se hace contra la demo
(<https://gespatiens-2026.vercel.app>), entrando con el botón "Entrar como Administración".

Cuando esté hecha, la fecha y lo que haya salido van en la declaración de accesibilidad
(`frontend/src/i18n/es.json` y `en.json`, apartado `accessibility`).

## Preparación

- **NVDA**: gratis en <https://www.nvaccess.org>. Se arranca con `Ctrl + Alt + N`. La tecla NVDA es `Insert`
  (o `Bloq Mayús` si se elige en la instalación).
- **Navegador**: Chrome o Firefox, con el zoom al 100 %.
- Teclas básicas de NVDA en una web:

| Tecla | Qué hace |
| --- | --- |
| `Tab` / `Mayús + Tab` | Siguiente / anterior elemento que se puede pulsar |
| `H` / `1`–`6` | Siguiente encabezado / de ese nivel |
| `D` | Siguiente región (navegación, contenido principal…) |
| `T` | Siguiente tabla. Dentro, `Ctrl + Alt + flechas` para moverse por celdas |
| `F` | Siguiente campo de formulario |
| `NVDA + F7` | Lista de enlaces, encabezados y regiones de la página |
| `Ctrl` | Calla a NVDA |

## 1. Solo con el teclado (sin NVDA)

Desenchufa el ratón o no lo toques.

- [ ] En el login, el primer `Tab` va a "Entrar como …" y se entra con `Intro`.
- [ ] Al entrar, el tutorial se recorre con `Tab` y se cierra con `Escape`; el foco vuelve a la página.
- [ ] El primer `Tab` de cada pantalla muestra "Saltar al contenido" y lleva al título.
- [ ] El foco se ve siempre (un recuadro azul) y nunca se pierde detrás de algo.
- [ ] El orden de `Tab` sigue el orden visual: menú, título, acciones, contenido.
- [ ] **Diario**: rellenar un turno, añadir una rebaja y pasar lista, todo con teclado.
- [ ] **Valoraciones**: escribir una nota, `Intro` la guarda, `Escape` la deshace.
- [ ] **Turnos**: en el cuadrante, poner y quitar un turno con `Intro` o `Espacio`.
- [ ] **Informes**: abrir uno, editar un apartado y cerrar el editor.
- [ ] Los diálogos (cerrar informe, dar de baja) se abren con el foco dentro, `Tab` no sale de ellos y
      `Escape` los cierra devolviendo el foco al botón que los abrió.
- [ ] Menú plegado y menú del móvil (ventana estrecha): se abren, se recorren y se cierran con `Escape`.
- [ ] Tema e idioma se cambian con teclado.

## 2. Con NVDA

- [ ] Al cargar cada pantalla, NVDA lee el título de la pestaña.
- [ ] `H` recorre los encabezados y tienen sentido solos ("Turnos", "Rebajas", "Actividades").
- [ ] `D` encuentra la navegación y el contenido principal.
- [ ] Los botones de solo icono dicen qué hacen ("Plegar el menú", "Día anterior").
- [ ] Los campos dicen su etiqueta y si son obligatorios. Al enviar un formulario con errores, se anuncia el error.
- [ ] Al guardar algo, se anuncia ("Guardado", "Informe cerrado").
- [ ] Las tablas (residentes, panel, valoraciones) se recorren con `Ctrl + Alt + flechas` y leen la cabecera de
      cada columna.
- [ ] En el cuadrante, cada botón dice turno, persona y día ("Mañana de Marta Serra el día 3, pulsado").
- [ ] En el login, si el servidor tarda, se anuncia "Conectando con el servidor".
- [ ] El cambio de tema se anuncia ("Tema: oscuro").

## 3. Zoom y texto grande

- [ ] Con el zoom del navegador al 200 %, todo se lee y se usa sin desplazamiento horizontal (salvo dentro de
      las tablas grandes).
- [ ] Con el zoom al 400 % (equivale a 320 px de ancho), la app pasa a la vista de móvil y sigue funcionando.
- [ ] Con el texto más grande de Windows (Configuración > Accesibilidad > Tamaño del texto, 150 %), nada se corta
      ni se solapa.

## 4. Preferencias del sistema

- [ ] Windows en modo oscuro y el tema en "del sistema": la app sale oscura.
- [ ] Con Configuración > Accesibilidad > Efectos visuales > "Efectos de animación" desactivado, no hay
      animaciones.
- [ ] Con un tema de contraste alto de Windows, se distinguen los botones, los campos y el foco.

## Resultado

| Fecha | Navegador y NVDA | Qué ha fallado | Arreglado en |
| --- | --- | --- | --- |
| | | | |
