# 0009. Eventos de Spring entre módulos

**Estado:** aceptada · **Fecha:** 2026-10

## Contexto

Algunas acciones de un módulo tienen efectos en otro. El caso principal: cuando un residente ingresa, deben
crearse su informe de media estancia y el de fin de programa; cuando sale, los informes reciben la fecha de salida.
Si el caso de uso de ingreso llamara directamente al módulo de informes, `patients` dependería de `reports`, y cada
módulo nuevo interesado en los ingresos obligaría a tocar el de residentes.

## Decisión

El dominio de `patients` define un puerto, `StayEvents`, con lo que ocurre en una estancia (`stayStarted`,
`stayEnded`). Su adaptador, `SpringStayEvents`, lo publica como **evento de Spring**. El módulo `reports` lo
escucha con `@EventListener` (`StayReportsListener`) y prepara los informes con su propio caso de uso.

## Alternativas consideradas

- **Llamada directa entre casos de uso.** Más sencilla de seguir, pero acopla los módulos en un solo sentido que
  tiende a convertirse en los dos.
- **Un broker de mensajes (RabbitMQ, Kafka).** Da durabilidad y reintentos, pero es infraestructura que un único
  servidor no necesita.
- **Que la pantalla de informes los cree al abrirse.** Funciona como respaldo, pero el panel de control necesita
  los informes desde el ingreso para avisar de los vencimientos.

## Consecuencias

- `patients` no sabe que existen los informes. Otro módulo puede escuchar los mismos eventos sin cambiarlo.
- Los eventos de Spring son síncronos y en memoria: si preparar los informes falla, el ingreso ya está hecho y no
  se deshace. El listener lo registra en el log, y las pantallas de informes completan lo que falte al abrirse.
- Seguir el flujo cuesta algo más: hay que saber que alguien escucha el evento.
