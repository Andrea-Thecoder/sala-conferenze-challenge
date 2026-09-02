-- pg_trgm: indicizza trigrammi di testo, necessario perché un indice B-tree
-- normale (vedi @Index su User/ConferenceHall) accelera solo i prefissi
-- ("ILIKE 'abc%'"), non i pattern con wildcard iniziale ("ILIKE '%abc%'").
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN preferito a GIST per ricerca testuale: lookup più veloci, update più
-- lenti — va bene qui perché anagrafica/sale si leggono molto più di quanto
-- si scrivano.
CREATE INDEX idx_app_user_first_name_trgm ON app_user USING gin (first_name gin_trgm_ops);
CREATE INDEX idx_app_user_last_name_trgm ON app_user USING gin (last_name gin_trgm_ops);
CREATE INDEX idx_app_user_email_trgm ON app_user USING gin (email gin_trgm_ops);

CREATE INDEX idx_conference_hall_name_trgm ON conference_hall USING gin (name gin_trgm_ops);

CREATE INDEX idx_building_street_trgm ON building USING gin (street gin_trgm_ops);
