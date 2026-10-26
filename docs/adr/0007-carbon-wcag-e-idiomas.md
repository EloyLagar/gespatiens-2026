# 0007. Carbon, WCAG 2.1 AA y dos idiomas desde el principio

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

La aplicación la usa todo el personal de un centro, durante toda la jornada y en equipos compartidos, así que tiene
que poder usarla cualquier persona del equipo, también quien navega con teclado, con lector de pantalla o con
zoom. La memoria del proyecto pide además que cada empleado pueda elegir el idioma (RNF9). La versión en Laravel
tenía un diseño propio, sin pautas de accesibilidad, y añadir accesibilidad o un segundo idioma a una interfaz ya
hecha cuesta mucho más que hacerlo desde el principio.

## Decisión

- **Carbon Design System** (IBM), con React: componentes con el teclado, el foco y ARIA resueltos, y una guía de
  diseño completa (`DESIGN.md`). Encima, los colores de la marca original en tokens propios.
- **WCAG 2.1 nivel AA** como requisito, no como mejora: todos los textos con contraste de al menos 4,5:1,
  calculado en cada token, en el tema claro y en el oscuro. Se añade el tamaño mínimo de los objetivos táctiles
  de WCAG 2.2.
- **Castellano e inglés** con i18next desde la primera pantalla: ningún texto escrito directamente en un
  componente. Cada empleado elige su idioma y se guarda en su perfil.
- **Comprobación automática**: axe en las pruebas de extremo a extremo de cada pantalla y Lighthouse en el CI
  (ver la [declaración de accesibilidad](https://gespatiens-2026.vercel.app/accesibilidad)).

## Alternativas consideradas

- **Material UI o Bootstrap.** También accesibles, pero Carbon está pensado para aplicaciones de gestión densas en
  datos (tablas, formularios largos), que es lo que es gespatiens.
- **Tailwind con componentes propios.** Más libertad visual, pero la accesibilidad de cada componente (diálogos,
  menús, avisos) habría que construirla y probarla a mano.
- **Solo castellano.** Más rápido, pero no cumplía el requisito de idioma de la memoria (RNF9).

## Consecuencias

- La interfaz se maneja con teclado y lector de pantalla y respeta el tema y el movimiento reducido del sistema.
- Un fallo de accesibilidad nuevo (un botón sin nombre, un color sin contraste) hace fallar el CI.
- Carbon pesa: su CSS es la mayor parte de lo que descarga el navegador la primera vez.
- Cada texto nuevo hay que escribirlo dos veces, en `es.json` y en `en.json`.
- Lo que axe no detecta (el orden del foco, cómo suena con un lector) necesita una revisión manual:
  [docs/accesibilidad-revision-manual.md](../accesibilidad-revision-manual.md).
