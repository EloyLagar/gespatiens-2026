-- Memoria final, 5.2: "muchos de los usuarios son obligados judicialmente a acceder al plan de desintoxicación
-- (...) La información que proporcionan las instituciones que administran estos perfiles suelen dar información
-- personalizada". La derivación de cada residente: quién lo deriva y, si la hay, la medida judicial
CREATE TABLE patient_referrals (
    patient_id            UUID          PRIMARY KEY,
    referring_institution VARCHAR(150),
    contact_name          VARCHAR(100),
    contact_phone         VARCHAR(20),
    court_ordered         BOOLEAN       NOT NULL,
    court                 VARCHAR(150),
    measure_end_date      DATE,
    notes                 VARCHAR(2000),
    updated_at            TIMESTAMPTZ   NOT NULL,

    CONSTRAINT fk_patient_referrals_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE,
    -- El juzgado y el fin de la medida solo tienen sentido si el ingreso es por medida judicial
    CONSTRAINT ck_patient_referrals_court CHECK (court_ordered OR (court IS NULL AND measure_end_date IS NULL))
);
