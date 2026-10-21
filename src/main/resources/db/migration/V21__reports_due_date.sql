-- Cuándo toca el informe de media estancia: a mitad del programa. Por defecto, a los 3 meses de la entrada,
-- y se puede cambiar en la cabecera de cada informe (decisión del centro). Solo lo tiene el de media estancia
ALTER TABLE reports ADD COLUMN due_date DATE;

UPDATE reports SET due_date = CAST(stay_start + INTERVAL '3 months' AS DATE) WHERE type = 'MID_STAY';

ALTER TABLE reports ADD CONSTRAINT ck_reports_due_date
    CHECK ((type = 'MID_STAY' AND due_date IS NOT NULL AND due_date >= stay_start)
        OR (type = 'FINAL' AND due_date IS NULL));
