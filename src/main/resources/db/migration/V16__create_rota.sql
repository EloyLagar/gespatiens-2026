-- El cuadrante de turnos: planificación, no registro de lo trabajado (decisión del centro: nada de fichajes)

-- A quién le toca qué parte de qué día. Al borrar al empleado se van sus turnos
CREATE TABLE planned_shifts (
    id          UUID        PRIMARY KEY,
    date        DATE        NOT NULL,
    part        VARCHAR(10) NOT NULL,
    employee_id UUID        NOT NULL,

    CONSTRAINT fk_planned_shifts_employee FOREIGN KEY (employee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_planned_shifts_part CHECK (part IN ('MORNING', 'AFTERNOON', 'NIGHT')),
    -- Nadie tiene dos veces el mismo turno
    CONSTRAINT uk_planned_shifts UNIQUE (date, part, employee_id)
);

CREATE INDEX ix_planned_shifts_date ON planned_shifts (date);

-- Los cambios de turno entre trabajadores. Si la administración quita un turno del cuadrante, sus cambios se van
CREATE TABLE shift_swaps (
    id                 UUID        PRIMARY KEY,
    requester_id       UUID        NOT NULL,
    requester_shift_id UUID        NOT NULL,
    partner_id         UUID        NOT NULL,
    -- NULL: el compañero solo cubre el turno, sin dar uno suyo a cambio
    partner_shift_id   UUID,
    status             VARCHAR(10) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    resolved_at        TIMESTAMPTZ,

    CONSTRAINT fk_shift_swaps_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_shift_swaps_partner FOREIGN KEY (partner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_shift_swaps_requester_shift FOREIGN KEY (requester_shift_id)
        REFERENCES planned_shifts (id) ON DELETE CASCADE,
    CONSTRAINT fk_shift_swaps_partner_shift FOREIGN KEY (partner_shift_id)
        REFERENCES planned_shifts (id) ON DELETE CASCADE,
    CONSTRAINT ck_shift_swaps_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    CONSTRAINT ck_shift_swaps_people CHECK (requester_id <> partner_id),
    CONSTRAINT ck_shift_swaps_resolved CHECK ((status = 'PENDING') = (resolved_at IS NULL))
);

CREATE INDEX ix_shift_swaps_requester ON shift_swaps (requester_id);
CREATE INDEX ix_shift_swaps_partner ON shift_swaps (partner_id);
