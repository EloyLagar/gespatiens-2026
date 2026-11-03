-- El registro de accesos anota también los cambios de contraseña. actor_id: quién la cambió, si no fue el propio
-- empleado (la administración que pone una provisional). Los nuevos resultados son más largos: PASSWORD_CHANGE_REJECTED
ALTER TABLE access_log ADD COLUMN actor_id UUID;
ALTER TABLE access_log ALTER COLUMN outcome TYPE VARCHAR(30);
