-- Permite emitir sin cliente y conserva los vínculos históricos y su clave foránea.
USE angur_gestion_admin;

ALTER TABLE cotizaciones
    MODIFY COLUMN cliente_id BIGINT UNSIGNED NULL;
