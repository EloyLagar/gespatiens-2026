-- Tutores de cada residente e intervenciones (Tutors e Interventions en la memoria)

-- Los empleados que tutelan a cada residente. Al borrar al empleado o al residente se va la relación
CREATE TABLE tutors (
    patient_id UUID NOT NULL,
    user_id    UUID NOT NULL,

    CONSTRAINT pk_tutors PRIMARY KEY (patient_id, user_id),
    CONSTRAINT fk_tutors_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_tutors_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_tutors_user ON tutors (user_id);

-- Lo que un tutor hizo con un residente un día. Que solo las registren sus tutores lo garantiza la aplicación
CREATE TABLE interventions (
    id         UUID          PRIMARY KEY,
    patient_id UUID          NOT NULL,
    -- NULL si el autor ya no está en la plantilla: la intervención se conserva aunque se borre al empleado
    author_id  UUID,
    date       DATE          NOT NULL,
    text       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    -- NULL mientras no se haya modificado
    edited_at  TIMESTAMPTZ,

    CONSTRAINT fk_interventions_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT fk_interventions_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_interventions_text_not_blank CHECK (length(trim(text)) > 0),
    CONSTRAINT ck_interventions_edited_after_created CHECK (edited_at IS NULL OR edited_at >= created_at)
);

-- El seguimiento de un residente las lista de la más reciente a la más antigua
CREATE INDEX ix_interventions_patient_date ON interventions (patient_id, date DESC, created_at DESC);
