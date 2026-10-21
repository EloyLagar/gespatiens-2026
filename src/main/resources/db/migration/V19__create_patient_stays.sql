-- Historial de estancias: hasta ahora un reingreso sustituía la fecha de entrada y se perdía la estancia
-- anterior. Aquí van las estancias ya terminadas; la actual (o la última) sigue en patients
CREATE TABLE patient_stays (
    patient_id UUID NOT NULL,
    entry_date DATE NOT NULL,
    exit_date  DATE NOT NULL,

    CONSTRAINT pk_patient_stays PRIMARY KEY (patient_id, entry_date),
    CONSTRAINT fk_patient_stays_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    CONSTRAINT ck_patient_stays_dates CHECK (exit_date >= entry_date)
);

-- Las estancias anteriores que ya se conocen: los informes se crean por estancia y guardan cuándo empezó y,
-- al terminar, la fecha de salida. Las que no tienen salida anotada no se pueden reconstruir
INSERT INTO patient_stays (patient_id, entry_date, exit_date)
SELECT r.patient_id, r.stay_start, MAX(r.exit_date)
FROM reports r
JOIN patients p ON p.id = r.patient_id
WHERE r.stay_start < p.entry_date
GROUP BY r.patient_id, r.stay_start
HAVING MAX(r.exit_date) IS NOT NULL AND MAX(r.exit_date) >= r.stay_start AND MAX(r.exit_date) <= MAX(p.entry_date);
