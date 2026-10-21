package com.viller.gespatiens.editing.domain;

// Dos empleados han pedido el mismo bloqueo en el mismo instante y la base de datos solo ha dejado a uno
public class LockRaceException extends RuntimeException {

    public LockRaceException(Throwable cause) {
        super("Otra persona ha empezado a editarlo en el mismo instante", cause);
    }
}
