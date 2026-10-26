# 0005. Esquema con Flyway y `ddl-auto=validate`

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

Hibernate puede crear y modificar las tablas solo (`ddl-auto=update`). Es práctico al empezar, pero no borra
columnas, no migra datos, puede tomar decisiones distintas en cada entorno y no deja historial de qué cambió ni
cuándo. Con una demo pública y, en el futuro, los datos reales de un centro, el esquema tiene que cambiar de forma
controlada y repetible.

## Decisión

El esquema lo crean y lo cambian **migraciones de Flyway** versionadas en
`src/main/resources/db/migration` (`V1__...sql` a `V25__users_theme.sql`). Cada cambio de la base de datos es una
migración nueva en el mismo commit que el código que la usa; las ya publicadas no se editan.

Hibernate arranca con **`ddl-auto=validate`**: solo comprueba que las entidades coinciden con las tablas. Si no
coinciden, la aplicación no arranca.

Los tests de persistencia usan su propio esquema (`gespatiens_test`), que Flyway crea y migra igual que el real.

## Alternativas consideradas

- **`ddl-auto=update`.** Sin historial, sin control de los datos y con resultados distintos según el entorno.
- **Liquibase.** Equivalente en capacidades. Flyway, con SQL plano, es más directo de leer y de revisar.
- **Scripts SQL a mano.** Nadie sabría qué scripts se han aplicado en cada base de datos.

## Consecuencias

- La misma secuencia de migraciones construye la base de datos de desarrollo, la de los tests, la del CI y la de
  la demo en Neon. Al desplegar, la API aplica sola las pendientes.
- Las restricciones viven en la base de datos (`CHECK`, `NOT NULL`, claves foráneas), además de en el dominio.
- Un error en una migración ya aplicada no se corrige editándola: hace falta otra migración que lo arregle.
- Añadir un campo cuesta más pasos (migración, entidad, adaptador), pero un desajuste se detecta al arrancar, no
  en producción.
