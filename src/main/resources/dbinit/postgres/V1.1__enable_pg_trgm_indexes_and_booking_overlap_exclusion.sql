CREATE
EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_app_user_first_name_trgm ON app_user USING gin (first_name gin_trgm_ops);
CREATE INDEX idx_app_user_last_name_trgm ON app_user USING gin (last_name gin_trgm_ops);
CREATE INDEX idx_app_user_email_trgm ON app_user USING gin (email gin_trgm_ops);

CREATE INDEX idx_conference_hall_name_trgm ON conference_hall USING gin (name gin_trgm_ops);

CREATE INDEX idx_building_street_trgm ON building USING gin (street gin_trgm_ops);

CREATE
EXTENSION IF NOT EXISTS btree_gist;


-- Vincolo contro la race condition su booking concorrenti:
-- ad ogni INSERT/UPDATE su a_booking, Postgres verifica che nessun'altra riga
-- per la stessa conference_hall_id abbia un tsrange(start,end) che si
-- sovrappone (operatore &&) a quello appena scritto. In caso di conflitto,
-- l'errore ha codice 23P01 (exclusion_violation) oppure 40P01.
-- Catturato in ServiceException.isOverlapViolation riconosce entrambi gli
-- SQLSTATE e li traduce nello stesso messaggio applicativo di conflitto.
ALTER TABLE a_booking
    ADD CONSTRAINT excl_a_booking_hall_time_overlap EXCLUDE USING gist (
    conference_hall_id WITH =,
    tsrange(start_date_time, end_date_time, '[)') WITH &&
  );
