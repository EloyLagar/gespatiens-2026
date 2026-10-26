# 0004. Casos de uso sin anotaciones de Spring

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

En Spring lo habitual es marcar cada servicio con `@Service` o `@Component` y dejar que el escaneo de paquetes lo
registre. Es cómodo, pero la capa de aplicación queda atada al framework: cada caso de uso importa anotaciones de
Spring, y la configuración (qué implementación de cada puerto se usa, qué valor tiene una propiedad) se reparte
entre muchas clases.

## Decisión

Los casos de uso (`application/`) son **clases Java normales**, sin anotaciones de Spring. Reciben sus puertos por
el constructor. Cada módulo tiene una clase `*BeanConfiguration` en `infrastructure/config/` que los crea con
`@Bean` y decide qué adaptador recibe cada uno.

Las propiedades también se resuelven ahí: por ejemplo, `UserBeanConfiguration` lee
`gespatiens.tutorial.every-login` con `@Value` y pasa un `boolean` a `MarkOwnTutorialSeenUseCase`.

## Alternativas consideradas

- **`@Service` en cada caso de uso.** Menos código, pero la capa de aplicación depende de Spring y la
  configuración queda implícita.
- **Un contenedor distinto, o módulos de Java.** Excesivo para el tamaño del proyecto.

## Consecuencias

- Los casos de uso se prueban con `new` y dobles de los puertos, sin arrancar Spring: los tests unitarios tardan
  milisegundos.
- La lista de beans de cada módulo está en un solo sitio y se lee de arriba abajo.
- Cada caso de uso nuevo hay que registrarlo a mano en su `*BeanConfiguration`. Si se olvida, la aplicación no
  arranca, y además los tests de los controladores necesitan su `@MockitoBean`. Es fácil de detectar, pero es un
  paso más.
