-- Demo reviews (D20): loads the demo reviews straight into the database.
--
-- Since D12 a client can only review a restaurant after attending a reservation there (ASISTIO),
-- and the API cannot create attended reservations in the past, so seed-demo.mjs cannot create
-- reviews on a fresh database. This script, for the demo database only, inserts for each demo
-- review one past ASISTIO reservation of that client at that restaurant and the review itself.
-- The API rule stays as it is.
--
-- Usage: run it after `node docs/db/seed-demo.mjs`, against the same database, for example
--   mysql -u <user> -p <database> < docs/db/seed-demo-resenas.sql
-- Run it again after every run of seed-demo.mjs: the seed deletes every reservation of the demo
-- restaurants (the past ASISTIO ones too) but keeps the reviews.
--
-- Idempotent: a reservation is only inserted when that client has no ASISTIO reservation at that
-- restaurant, and a review only when that client has no review of that restaurant. Running it twice
-- inserts nothing the second time.
-- Only demo rows: every client and restaurant is looked up by its @demo.foodies.com account;
-- nothing else is read or changed. A demo account that does not exist is skipped.

SET NAMES utf8mb4;
-- DAYNAME() must return English names to match turnos.dia (MONDAY, TUESDAY, ...).
SET lc_time_names = 'en_US';

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS demo_resenas;
CREATE TEMPORARY TABLE demo_resenas (
  cliente      VARCHAR(255) NOT NULL,
  restaurante  VARCHAR(255) NOT NULL,
  calificacion INT          NOT NULL,
  comentario   VARCHAR(255) NOT NULL,
  dias         INT          NOT NULL,
  horario      TIME         NOT NULL
);

-- Client account, restaurant manager account, rating, comment (the texts of seed-demo.mjs),
-- days ago of the attended reservation and its arrival time (a demo shift start).
INSERT INTO demo_resenas VALUES
  ('ana.gomez@demo.foodies.com',     'labrasadeguemes@demo.foodies.com',    5, 'El ojo de bife salió en su punto justo. Atención cálida y rápida.', 12, '21:30'),
  ('bruno.diaz@demo.foodies.com',    'labrasadeguemes@demo.foodies.com',    4, 'Muy buena parrilla, las mollejas son imperdibles. Un poco ruidoso el salón.', 20, '20:00'),
  ('carla.ruiz@demo.foodies.com',    'labrasadeguemes@demo.foodies.com',    5, 'Fuimos por un cumpleaños y nos atendieron de diez.', 33, '21:30'),
  ('diego.sosa@demo.foodies.com',    'donramonparrilla@demo.foodies.com',   4, 'Porciones enormes y buen precio. Ideal para ir en grupo.', 9, '13:30'),
  ('elena.paz@demo.foodies.com',     'donramonparrilla@demo.foodies.com',   3, 'La carne bien, pero esperamos casi una hora un sábado.', 26, '21:30'),
  ('ana.gomez@demo.foodies.com',     'kaiyosushibar@demo.foodies.com',      5, 'El mejor sushi de la zona de Playa Grande, todo muy fresco.', 40, '20:00'),
  ('facundo.rios@demo.foodies.com',  'kaiyosushibar@demo.foodies.com',      4, 'Muy rico el ramen; el local es chico, conviene reservar.', 15, '21:30'),
  ('bruno.diaz@demo.foodies.com',    'trattorianonnarosa@demo.foodies.com', 5, 'Los sorrentinos de calabaza son como los de la abuela.', 7, '13:30'),
  ('gala.torres@demo.foodies.com',   'trattorianonnarosa@demo.foodies.com', 4, 'Pastas caseras y buena carta de vinos.', 18, '20:00'),
  ('carla.ruiz@demo.foodies.com',    'cantinadelpuerto@demo.foodies.com',   4, 'Rabas crocantes y la cazuela de mariscos muy completa.', 22, '12:00'),
  ('diego.sosa@demo.foodies.com',    'cantinadelpuerto@demo.foodies.com',   3, 'Rico el pescado, pero el servicio fue lento.', 30, '21:30'),
  ('elena.paz@demo.foodies.com',     'cafedelarambla@demo.foodies.com',     5, 'Café de especialidad con vista al mar. Las medialunas, recién hechas.', 5, '10:00'),
  ('facundo.rios@demo.foodies.com',  'cafedelarambla@demo.foodies.com',     4, 'Buen lugar para trabajar un rato a la mañana.', 11, '09:00'),
  ('gala.torres@demo.foodies.com',   'elrincondemirta@demo.foodies.com',    4, 'La milanesa a caballo es enorme y sale rapidísimo.', 14, '13:30'),
  ('hernan.vega@demo.foodies.com',   'elrincondemirta@demo.foodies.com',    2, 'Pésima atención, el mozo nos trató de mala manera. No vuelvo nunca más, son unos inútiles.', 3, '21:30');

-- 1. One past attended reservation per review, in the restaurant's first space and, when the
--    restaurant has a shift starting at that time on that weekday, in that shift.
INSERT INTO reserva (cantidad, estado_reserva, fecha_reserva, horariollegada, espacio_id, restaurant_id, usuario_id, turno_id)
SELECT 2,
       'ASISTIO',
       TIMESTAMP(CURDATE() - INTERVAL d.dias DAY),
       d.horario,
       (SELECT MIN(e.id) FROM espacios e WHERE e.restaurant_id = r.id),
       r.id,
       c.id,
       (SELECT MIN(t.id) FROM turnos t
         WHERE t.restaurant_id = r.id
           AND t.inicio = d.horario
           AND t.dia = UPPER(DAYNAME(CURDATE() - INTERVAL d.dias DAY)))
FROM demo_resenas d
JOIN usuario c ON c.email = d.cliente AND c.rol = 'ROLE_CLIENTE'
JOIN usuario m ON m.email = d.restaurante
JOIN restaurants r ON r.usuario_id = m.id
WHERE c.email LIKE '%@demo.foodies.com'
  AND m.email LIKE '%@demo.foodies.com'
  AND NOT EXISTS (SELECT 1 FROM reserva x
                   WHERE x.usuario_id = c.id AND x.restaurant_id = r.id AND x.estado_reserva = 'ASISTIO');

-- 2. The review, written the day after the visit.
INSERT INTO resena (calificacion, comentario, fecha_creacion, restaurant_id, usuario_id)
SELECT d.calificacion,
       d.comentario,
       TIMESTAMP(CURDATE() - INTERVAL (d.dias - 1) DAY, '12:00:00'),
       r.id,
       c.id
FROM demo_resenas d
JOIN usuario c ON c.email = d.cliente AND c.rol = 'ROLE_CLIENTE'
JOIN usuario m ON m.email = d.restaurante
JOIN restaurants r ON r.usuario_id = m.id
WHERE c.email LIKE '%@demo.foodies.com'
  AND m.email LIKE '%@demo.foodies.com'
  AND NOT EXISTS (SELECT 1 FROM resena x WHERE x.usuario_id = c.id AND x.restaurant_id = r.id);

COMMIT;

-- Summary: demo reviews and past attended demo reservations now in the database.
SELECT
  (SELECT COUNT(*) FROM resena x JOIN usuario c ON c.id = x.usuario_id
    WHERE c.email LIKE '%@demo.foodies.com') AS resenas_demo,
  (SELECT COUNT(*) FROM reserva x JOIN usuario c ON c.id = x.usuario_id
    WHERE c.email LIKE '%@demo.foodies.com' AND x.estado_reserva = 'ASISTIO') AS asistencias_demo;

DROP TEMPORARY TABLE IF EXISTS demo_resenas;
