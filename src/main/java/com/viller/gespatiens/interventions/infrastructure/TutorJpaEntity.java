package com.viller.gespatiens.interventions.infrastructure;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// Una fila de tutors: un empleado que tutela a un residente. El agregado ResidentTutors son todas las
// filas de un residente; las arma ResidentTutorsRepositoryJpaAdapter
@Entity
@Table(name = "tutors")
@IdClass(TutorJpaEntity.Key.class)
public class TutorJpaEntity {

    @Id
    private UUID patientId;

    @Id
    private UUID userId;

    protected TutorJpaEntity() {
    }

    public TutorJpaEntity(UUID patientId, UUID userId) {
        this.patientId = patientId;
        this.userId = userId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getUserId() {
        return userId;
    }

    // Clave compuesta (patient_id, user_id). JPA necesita constructor vacío, equals y hashCode
    public static class Key implements Serializable {

        private UUID patientId;
        private UUID userId;

        public Key() {
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other && Objects.equals(patientId, other.patientId)
                    && Objects.equals(userId, other.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(patientId, userId);
        }
    }
}
