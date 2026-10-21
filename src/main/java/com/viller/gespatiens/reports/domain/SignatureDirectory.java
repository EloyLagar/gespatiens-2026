package com.viller.gespatiens.reports.domain;

import java.util.Optional;
import java.util.UUID;

// Puerto hacia el módulo de empleados: la firma escaneada (PNG) que se imprime junto al nombre de cada autor
public interface SignatureDirectory {
    Optional<byte[]> signatureOf(UUID employeeId);
}
