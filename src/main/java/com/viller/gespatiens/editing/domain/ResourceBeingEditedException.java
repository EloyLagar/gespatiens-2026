package com.viller.gespatiens.editing.domain;

import java.time.Instant;

// RF8: otra persona lo está editando. Se dice quién y desde cuándo para que se pueda hablar con ella
public class ResourceBeingEditedException extends RuntimeException {

    // null si quien lo edita ya no está en la plantilla (no debería pasar: el bloqueo se borra con él)
    private final String holderName;
    private final Instant since;

    public ResourceBeingEditedException(String holderName, Instant since) {
        super((holderName == null ? "Otra persona" : holderName) + " lo está editando ahora mismo. "
                + "Podrás modificarlo cuando termine");
        this.holderName = holderName;
        this.since = since;
    }

    public String getHolderName() {
        return holderName;
    }

    public Instant getSince() {
        return since;
    }
}
