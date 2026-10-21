package com.viller.gespatiens.access.users.application;

public class PasswordSetupDeliveryException extends RuntimeException {
    public PasswordSetupDeliveryException(String email, Throwable cause) {
        super("No se ha podido enviar el correo a " + email + ". Revisa la configuración del correo e inténtalo de nuevo",
                cause);
    }
}
