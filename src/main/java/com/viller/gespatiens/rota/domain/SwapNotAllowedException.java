package com.viller.gespatiens.rota.domain;

// Un cambio de turno que no se puede proponer, aceptar, rechazar o retirar (con el motivo)
public class SwapNotAllowedException extends RuntimeException {

    public SwapNotAllowedException(String message) {
        super(message);
    }
}
