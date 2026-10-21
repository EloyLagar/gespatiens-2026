package com.viller.gespatiens.access.users.domain;

// El enlace no existe, ya se ha usado, lo ha sustituido otro más reciente o ha caducado.
// No se distingue entre los casos para no dar pistas a quien pruebe tokens
public class InvalidPasswordSetupTokenException extends RuntimeException {
    public InvalidPasswordSetupTokenException() {
        super("El enlace para crear la contraseña no es válido o ha caducado. Pide a la administración que te envíe otro");
    }
}
