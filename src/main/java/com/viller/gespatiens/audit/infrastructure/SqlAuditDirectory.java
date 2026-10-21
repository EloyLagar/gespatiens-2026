package com.viller.gespatiens.audit.infrastructure;

import com.viller.gespatiens.audit.application.AuditDirectory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Nombres y relaciones con consultas de solo lectura sobre las tablas de los otros módulos, como el panel
@Component
public class SqlAuditDirectory implements AuditDirectory {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Map<UUID, String> employeeNames(Collection<UUID> ids) {
        Map<UUID, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        List<Object[]> rows = entityManager.createNativeQuery("SELECT id, name FROM users WHERE id IN (:ids)")
                .setParameter("ids", ids)
                .getResultList();
        rows.forEach(r -> names.put((UUID) r[0], (String) r[1]));
        return names;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<UUID, Resident> residents(Collection<UUID> ids) {
        Map<UUID, Resident> residents = new HashMap<>();
        if (ids.isEmpty()) {
            return residents;
        }
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT id, number, name, surname FROM patients WHERE id IN (:ids)")
                .setParameter("ids", ids)
                .getResultList();
        rows.forEach(r -> residents.put((UUID) r[0], new Resident((UUID) r[0],
                r[1] == null ? null : ((Number) r[1]).intValue(), (String) r[2], (String) r[3])));
        return residents;
    }

    @Override
    public Optional<UUID> residentOfReport(UUID reportId) {
        return single("SELECT patient_id FROM reports WHERE id = :id", reportId);
    }

    @Override
    public Optional<UUID> residentOfIntervention(UUID interventionId) {
        return single("SELECT patient_id FROM interventions WHERE id = :id", interventionId);
    }

    @SuppressWarnings("unchecked")
    private Optional<UUID> single(String sql, UUID id) {
        List<Object> rows = entityManager.createNativeQuery(sql).setParameter("id", id).getResultList();
        return rows.stream().findFirst().map(UUID.class::cast);
    }
}
