-- Registro de accesos a los datos sensibles de los residentes (RNF7, RGPD): quién consultó o cambió qué y
-- cuándo. Sin claves ajenas a propósito: si se borra al empleado o al residente, el registro se conserva
CREATE TABLE audit_log (
    id          UUID        PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_id    UUID        NOT NULL,
    action      VARCHAR(30) NOT NULL,
    patient_id  UUID,
    resource_id UUID
);

-- Las tres consultas de la pantalla: todo, lo de un residente y lo de un empleado, de lo más reciente atrás
CREATE INDEX ix_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX ix_audit_log_patient ON audit_log (patient_id, occurred_at DESC);
CREATE INDEX ix_audit_log_actor ON audit_log (actor_id, occurred_at DESC);
