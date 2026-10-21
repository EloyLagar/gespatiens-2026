-- Hasta ahora el alta y el reingreso aceptaban una fecha de entrada futura. Un residente así no se podía dar
-- de baja (la salida no puede ser futura ni anterior a la entrada) ni, por tanto, borrar. Las fechas futuras
-- que se colaron pasan a hoy, y con ellas el inicio de la estancia de sus informes
UPDATE reports r
SET stay_start = CURRENT_DATE,
    entry_date = CASE WHEN r.entry_date > CURRENT_DATE THEN CURRENT_DATE ELSE r.entry_date END
FROM patients p
WHERE r.patient_id = p.id
  AND p.entry_date > CURRENT_DATE
  AND r.stay_start = p.entry_date;

UPDATE patients SET entry_date = CURRENT_DATE WHERE entry_date > CURRENT_DATE;
