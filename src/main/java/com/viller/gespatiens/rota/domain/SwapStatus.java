package com.viller.gespatiens.rota.domain;

public enum SwapStatus {
    // Esperando a que el compañero responda
    PENDING,
    ACCEPTED,
    DECLINED,
    // Lo retiró quien lo propuso, o dejó de tener sentido porque el cuadrante cambió
    CANCELLED
}
