package com.viller.gespatiens.reports.infrastructure;

import com.viller.gespatiens.access.users.domain.Signature;
import com.viller.gespatiens.access.users.domain.SignatureRepository;
import com.viller.gespatiens.reports.domain.SignatureDirectory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

// Adaptador entre módulos: la firma escaneada que cada empleado sube desde su perfil
@Component
public class ReportsSignatureDirectory implements SignatureDirectory {

    private final SignatureRepository signatureRepository;

    public ReportsSignatureDirectory(SignatureRepository signatureRepository) {
        this.signatureRepository = signatureRepository;
    }

    @Override
    public Optional<byte[]> signatureOf(UUID employeeId) {
        return signatureRepository.findByUser(employeeId).map(Signature::getPng);
    }
}
