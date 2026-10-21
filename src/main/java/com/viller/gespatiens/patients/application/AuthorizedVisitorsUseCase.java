package com.viller.gespatiens.patients.application;

import com.viller.gespatiens.patients.domain.AuthorizedVisitor;
import com.viller.gespatiens.patients.domain.AuthorizedVisitorNotFoundException;
import com.viller.gespatiens.patients.domain.AuthorizedVisitorRepository;
import com.viller.gespatiens.patients.domain.PatientNotFoundException;
import com.viller.gespatiens.patients.domain.PatientRepository;

import java.util.List;
import java.util.UUID;

// Las personas autorizadas a visitar a un residente: "tendrán un apartado dentro de su ficha en el que se
// podrá gestionar las personas que están autorizadas a visitarles". Las consulta todo el personal y las
// gestiona la administración (SecurityConfig)
public class AuthorizedVisitorsUseCase {

    // Una lista razonable: más que esto es un error de quien la rellena
    public static final int MAX_PER_RESIDENT = 20;

    private final PatientRepository patientRepository;
    private final AuthorizedVisitorRepository visitorRepository;

    public AuthorizedVisitorsUseCase(PatientRepository patientRepository,
                                     AuthorizedVisitorRepository visitorRepository) {
        this.patientRepository = patientRepository;
        this.visitorRepository = visitorRepository;
    }

    public List<AuthorizedVisitor> list(UUID patientId) {
        ensurePatient(patientId);
        return visitorRepository.findByPatient(patientId);
    }

    public AuthorizedVisitor authorize(UUID patientId, String name, String relationship, String phoneNumber) {
        ensurePatient(patientId);
        if (visitorRepository.findByPatient(patientId).size() >= MAX_PER_RESIDENT) {
            throw new IllegalArgumentException("Un residente no puede tener más de " + MAX_PER_RESIDENT
                    + " personas autorizadas");
        }
        return visitorRepository.save(AuthorizedVisitor.authorize(patientId, name, relationship, phoneNumber));
    }

    public AuthorizedVisitor update(UUID patientId, UUID visitorId, String name, String relationship,
                                    String phoneNumber) {
        AuthorizedVisitor visitor = visitorOf(patientId, visitorId);
        visitor.update(name, relationship, phoneNumber);
        return visitorRepository.save(visitor);
    }

    public void revoke(UUID patientId, UUID visitorId) {
        visitorRepository.delete(visitorOf(patientId, visitorId));
    }

    // Que la persona sea de ese residente: la URL lleva los dos ids
    private AuthorizedVisitor visitorOf(UUID patientId, UUID visitorId) {
        return visitorRepository.findById(visitorId)
                .filter(v -> v.getPatientId().equals(patientId))
                .orElseThrow(() -> new AuthorizedVisitorNotFoundException(visitorId));
    }

    private void ensurePatient(UUID patientId) {
        if (patientRepository.findById(patientId).isEmpty()) {
            throw new PatientNotFoundException(patientId);
        }
    }
}
