-- Memoria final: cada residente tiene un informe de cada tipo por estancia, que se crea solo al entrar.
-- Si sale y vuelve, los de la estancia anterior se conservan como historial (RF2 los reúne en un PDF)

ALTER TABLE reports ADD COLUMN stay_start DATE;

-- Los informes que ya existían son de la estancia en la que se empezaron
UPDATE reports SET stay_start = COALESCE(entry_date, CAST(created_at AS DATE));

ALTER TABLE reports ALTER COLUMN stay_start SET NOT NULL;

-- Ya no se empiezan a mano: la estancia identifica el informe
DROP INDEX ux_reports_one_draft_per_type;
CREATE UNIQUE INDEX ux_reports_one_per_stay ON reports (patient_id, type, stay_start);
