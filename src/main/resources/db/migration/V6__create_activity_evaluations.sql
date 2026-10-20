-- RF6: valoración de cada residente que asistió a una actividad (Evaluations y Lessons en la memoria).
-- La nota la pone el personal; la satisfacción y la utilidad las da el residente en papel y las pasa el
-- educador. Las tres de 0 a 10 con un decimal; basta con una
CREATE TABLE activity_evaluations (
    activity_id  UUID         NOT NULL,
    patient_id   UUID         NOT NULL,
    mark         NUMERIC(3,1),
    satisfaction NUMERIC(3,1),
    utility      NUMERIC(3,1),

    CONSTRAINT pk_activity_evaluations PRIMARY KEY (activity_id, patient_id),
    CONSTRAINT ck_activity_evaluations_some CHECK (mark IS NOT NULL OR satisfaction IS NOT NULL OR utility IS NOT NULL),
    CONSTRAINT ck_activity_evaluations_mark CHECK (mark BETWEEN 0 AND 10),
    CONSTRAINT ck_activity_evaluations_satisfaction CHECK (satisfaction BETWEEN 0 AND 10),
    CONSTRAINT ck_activity_evaluations_utility CHECK (utility BETWEEN 0 AND 10),
    -- Que solo se valore a quien asistió lo garantiza Activity
    CONSTRAINT fk_activity_evaluations_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_evaluations_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
);
