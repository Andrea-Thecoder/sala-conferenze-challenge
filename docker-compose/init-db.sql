-- Docker creates database and user automatically via POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD
-- This file only creates the schema and grants

-- ── Schema ──────────────────────────────
CREATE SCHEMA IF NOT EXISTS dev;
GRANT ALL ON SCHEMA dev TO "sala-backend-challenge";
ALTER USER "sala-backend-challenge" SET search_path TO dev, public;

-- Add your schema tables here
