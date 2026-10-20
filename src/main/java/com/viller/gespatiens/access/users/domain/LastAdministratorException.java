package com.viller.gespatiens.access.users.domain;

public class LastAdministratorException extends RuntimeException {

    public LastAdministratorException() {
        super("Es el último administrador: sin él nadie podría gestionar los empleados");
    }
}
