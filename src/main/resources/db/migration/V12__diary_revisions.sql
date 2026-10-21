-- RF8 en el diario: cada turno y cada actividad lleva la cuenta de cuántas veces se ha guardado. Quien guarda
-- dice qué versión tenía delante y, si otra persona lo cambió mientras tanto, no se pisa lo suyo
ALTER TABLE shift_logs ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE activities ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
