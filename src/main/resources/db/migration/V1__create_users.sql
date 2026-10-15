-- Empleados del centro (agregado User del módulo access/users)
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(255) NOT NULL,
    speciality    VARCHAR(30)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    CONSTRAINT uk_users_email        UNIQUE (email),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
    CONSTRAINT ck_users_speciality   CHECK (speciality IN
        ('ADMINISTRATOR', 'SOCIAL_EDUCATOR', 'SOCIAL_WORKER', 'PSYCHOLOGIST', 'MEDICAL_TEAM'))
);
