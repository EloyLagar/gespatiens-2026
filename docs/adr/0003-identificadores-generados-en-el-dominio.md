# 0003. Identificadores UUID generados en el dominio

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

Con `@GeneratedValue` es la base de datos quien da el identificador, al guardar. Hasta entonces el agregado no
tiene identidad: no se puede referenciar desde otro agregado, publicar en un evento ni devolver en una respuesta
sin haber pasado antes por la base de datos. Además, el dominio pasa a depender de un detalle de persistencia.

## Decisión

Los agregados generan su propio identificador, un **UUID**, en su método de fábrica
([0002](0002-agregados-con-metodos-de-fabrica.md)). La base de datos lo guarda como clave primaria de tipo `uuid`
y las entidades JPA no llevan `@GeneratedValue`.

## Alternativas consideradas

- **Secuencias o `IDENTITY` de PostgreSQL.** Identificadores cortos y ordenados, pero solo existen después de
  guardar, y son predecibles en las URLs (`/residentes/7` invita a probar `/residentes/8`).
- **Un servicio de identificadores inyectado en el dominio.** Más flexible para tests deterministas, pero añade
  una dependencia a cada fábrica sin ganar nada práctico en esta aplicación.

## Consecuencias

- Un agregado tiene identidad desde que se crea: un caso de uso puede crear un residente y avisar de su ingreso
  a otros módulos ([0009](0009-eventos-entre-modulos.md)) con el identificador ya conocido.
- Las URLs no revelan cuántos registros hay ni permiten adivinar los de otros.
- Los UUID ocupan más que un entero y no son legibles. Por eso las pantallas muestran el número de plaza del
  residente, que es lo que el personal reconoce, y no el identificador.
