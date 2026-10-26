-- Local development database and user for the Foodies backend.
-- Run once as the MySQL root user, for example from MySQL Workbench or:
--   mysql -u root -p < docs/db/crear-usuario-local.sql
--
-- Before running it, replace CAMBIAR_ESTA_CLAVE with a password of your choice.
-- Then start the backend with these environment variables:
--   DB_HOST=localhost  DB_PORT=3306  DB_NAME=foodies  DB_USER=foodies_app  DB_PASS=<the same password>

CREATE DATABASE IF NOT EXISTS foodies
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'foodies_app'@'localhost' IDENTIFIED BY 'CAMBIAR_ESTA_CLAVE';

-- Only the foodies schema: the app needs to create and alter its tables (ddl-auto=update).
GRANT ALL PRIVILEGES ON foodies.* TO 'foodies_app'@'localhost';

FLUSH PRIVILEGES;
