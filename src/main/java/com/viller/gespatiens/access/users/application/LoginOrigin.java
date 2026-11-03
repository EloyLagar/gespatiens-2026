package com.viller.gespatiens.access.users.application;

// Desde dónde se intenta entrar: la dirección IP y el navegador, tal como llegan en la petición. Lo rellena la capa
// web; cualquiera de los dos puede ser null si no se sabe
public record LoginOrigin(String ipAddress, String userAgent) {

    public static final LoginOrigin UNKNOWN = new LoginOrigin(null, null);
}
