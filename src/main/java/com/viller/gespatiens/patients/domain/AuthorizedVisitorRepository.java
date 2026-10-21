package com.viller.gespatiens.patients.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizedVisitorRepository {
    AuthorizedVisitor save(AuthorizedVisitor visitor);
    Optional<AuthorizedVisitor> findById(UUID id);
    // Por nombre
    List<AuthorizedVisitor> findByPatient(UUID patientId);
    void delete(AuthorizedVisitor visitor);
}
