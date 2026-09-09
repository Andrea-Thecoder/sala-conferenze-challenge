CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_app_user_first_name_trgm ON app_user USING gin (first_name gin_trgm_ops);
CREATE INDEX idx_app_user_last_name_trgm ON app_user USING gin (last_name gin_trgm_ops);
CREATE INDEX idx_app_user_email_trgm ON app_user USING gin (email gin_trgm_ops);

CREATE INDEX idx_conference_hall_name_trgm ON conference_hall USING gin (name gin_trgm_ops);

CREATE INDEX idx_building_street_trgm ON building USING gin (street gin_trgm_ops);

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE a_booking
  ADD CONSTRAINT excl_a_booking_hall_time_overlap
  EXCLUDE USING gist (
    conference_hall_id WITH =,
    tsrange(start_date_time, end_date_time, '[)') WITH &&
  );
