-- Igual que infra/postgres/initdb/01-crear-owner.sh en docker compose: el owner
-- de la base, que corre las migraciones, es un rol dedicado sin SUPERUSER ni
-- BYPASSRLS. CREATEROLE le permite crear y administrar los roles gn_*.
CREATE ROLE gn_owner LOGIN CREATEROLE PASSWORD 'gn_owner_test';
ALTER DATABASE test OWNER TO gn_owner;
