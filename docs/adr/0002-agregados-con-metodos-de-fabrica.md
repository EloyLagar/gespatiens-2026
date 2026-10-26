# 0002. Agregados con constructor privado y métodos de fábrica

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

Un agregado (un empleado, un residente, un informe) tiene reglas que siempre deben cumplirse: un email válido, un
DNI con su letra correcta, una estancia que no acaba antes de empezar. Con un constructor público y *setters*,
cualquier parte del código puede crear un objeto a medias o dejarlo en un estado imposible, y la regla deja de
estar en un solo sitio.

Además, un agregado nace de dos maneras distintas: cuando alguien lo crea en la aplicación (hay que validar y
generar su identificador) y cuando se lee de la base de datos (ya era válido y ya tiene identificador).

## Decisión

Los agregados tienen el **constructor privado** y se obtienen con **métodos de fábrica** con nombre:

- `create(...)`, o un nombre del negocio como `User.invite(...)` para un empleado que aún no tiene contraseña:
  valida las reglas, genera el identificador y deja el objeto en su estado inicial.
- `reconstitute(...)`: lo usan solo los adaptadores de persistencia para rehacer un agregado guardado, sin volver a
  aplicar las reglas de creación.

Los cambios de estado son métodos con significado (`markTutorialSeen()`, `changeTheme(theme)`) que comprueban sus
propias reglas, no *setters*.

## Alternativas consideradas

- **Constructor público con validación.** No distingue crear de reconstituir: o se valida dos veces o se
  complica el constructor con parámetros para saltarse la validación.
- **Entidades JPA anémicas con la lógica en servicios.** Es el patrón más extendido en Spring, pero las reglas se
  dispersan, y es justo lo que la reescritura quería evitar ([0001](0001-arquitectura-hexagonal-por-modulos.md)).
- **Builders.** Útiles con muchos parámetros opcionales, pero permiten construir objetos incompletos si no se
  controlan con cuidado.

## Consecuencias

- Un agregado inválido no puede existir en memoria: si `create` devuelve un objeto, cumple las reglas.
- El código cuenta el negocio: `User.invite(...)` dice más que un constructor con una contraseña a `null`.
- `reconstitute` tiene listas de parámetros largas y crece con cada campo nuevo (por ejemplo, al añadir el tema o
  el tutorial visto). Hay que actualizar a la vez el agregado, la entidad JPA, el adaptador y los tests.
