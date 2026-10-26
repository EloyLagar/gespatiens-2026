# 0001. Arquitectura hexagonal por módulos en lugar del MVC por capas de Laravel

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

gespatiens nació como proyecto final del ciclo de DAW (IES Serpis, 2023/24), hecho en Laravel con el MVC habitual:
controladores, modelos Eloquent y vistas Blade. Las reglas del centro (quién redacta cada apartado de un informe,
cuándo vence el de media estancia, quién puede rebajar a un residente) estaban repartidas entre controladores y
modelos, mezcladas con el acceso a la base de datos. Cambiar una regla obligaba a buscarla en varios sitios, y
probarla obligaba a levantar la base de datos.

La reescritura tenía dos objetivos: que esas reglas fueran fáciles de encontrar, cambiar y probar, y aprender una
arquitectura que se usa en proyectos grandes.

## Decisión

El backend se divide en **módulos por contexto delimitado** (`access`, `patients`, `diary`, `interventions`,
`reports`, `rota`, `notices`, `dashboard`, `editing`, `audit`) y cada uno sigue la **arquitectura hexagonal**
(puertos y adaptadores):

- `domain/`: agregados, objetos de valor, excepciones de negocio y **puertos** (interfaces como `UserRepository`).
  No depende de Spring ni de JPA.
- `application/`: casos de uso, uno por acción del usuario (`AdmitPatientUseCase`, `CloseReportUseCase`).
  Orquestan el dominio a través de los puertos.
- `infrastructure/`: **adaptadores**. Controladores REST, entidades y repositorios JPA, seguridad, correo, PDF y
  la configuración de Spring.

Las dependencias apuntan siempre hacia dentro: la infraestructura conoce al dominio, nunca al revés.

## Alternativas consideradas

- **MVC por capas (controller, service, repository) en Spring.** Es lo más habitual y lo más rápido de empezar,
  pero las entidades JPA acaban siendo el modelo de dominio y las reglas se reparten en servicios enormes. Es el
  mismo problema que tenía la versión en Laravel.
- **Seguir con Laravel y ordenarlo mejor.** Habría servido para el centro, pero no cumplía el objetivo de aprender
  Java y Spring.
- **Microservicios.** Un centro con unas decenas de usuarios no lo necesita: un único despliegue es más barato y
  sencillo. Los módulos ya separan los contextos, así que se podrían extraer si algún día hiciera falta.

## Consecuencias

- Las reglas de negocio están en el dominio y se prueban con tests unitarios rápidos, sin Spring ni base de datos.
- Cambiar la base de datos o el framework web solo toca los adaptadores.
- Hay más código que en un CRUD: cada agregado tiene su entidad JPA y su mapeo de ida y vuelta
  (`UserJpaEntity` y `User`). Es el precio de que el dominio no dependa de JPA.
- Las pantallas que leen datos de varios módulos (el panel de control) usan consultas SQL de solo lectura en vez
  de pasar por los agregados: los agregados protegen las escrituras, y no hace falta cargarlos para un listado.
