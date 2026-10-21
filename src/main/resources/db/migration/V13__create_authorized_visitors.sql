-- Personas autorizadas a visitar a cada residente (Visitors en la memoria final). Al borrar la ficha se van
CREATE TABLE authorized_visitors (
    id           UUID        PRIMARY KEY,
    patient_id   UUID        NOT NULL,
    name         VARCHAR(50) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,

    CONSTRAINT fk_authorized_visitors_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);

CREATE INDEX ix_authorized_visitors_patient ON authorized_visitors (patient_id);
