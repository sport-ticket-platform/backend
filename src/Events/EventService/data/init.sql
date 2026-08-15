-- =========================================================
-- init.sql
-- Creates base tables (respecting FK dependency order) and
-- seeds them with some default values.
-- =========================================================

-- ---------------------------------------------------------
-- Independent lookup tables first
-- ---------------------------------------------------------

CREATE TABLE city
(
    city_id SERIAL PRIMARY KEY,
    name    varchar(50) NOT NULL
);

CREATE TABLE sport
(
    sport_id   SERIAL PRIMARY KEY,
    sport_name varchar(50) NOT NULL
);

CREATE TABLE ticket_category
(
    category_id SERIAL PRIMARY KEY,
    name        varchar(50) NOT NULL
);

-- ---------------------------------------------------------
-- Tables depending on city / sport
-- ---------------------------------------------------------

CREATE TABLE team
(
    team_id  SERIAL PRIMARY KEY,
    name     varchar(50) NOT NULL,
    sport_id INT         NOT NULL REFERENCES sport (sport_id),
    city_id  INT         NOT NULL REFERENCES city (city_id)
);

CREATE TABLE venue
(
    venue_id    SERIAL PRIMARY KEY,
    name        varchar(50)  NOT NULL,
    city_id     INT          NOT NULL REFERENCES city (city_id),
    street      varchar(255) NOT NULL,
    postal_code varchar(15)  NOT NULL,
    latitude    decimal      NOT NULL,
    longitude   decimal      NOT NULL
);

CREATE TABLE league
(
    league_id SERIAL PRIMARY KEY,
    name      varchar(50) NOT NULL,
    sport_id  int4        NOT NULL REFERENCES sport (sport_id),
    UNIQUE (sport_id, name)
);

-- ---------------------------------------------------------
-- match depends on league, sport, venue, team
-- ---------------------------------------------------------

CREATE TABLE "match"
(
    match_id      BIGSERIAL PRIMARY KEY,
    league_id     INT         NOT NULL REFERENCES league (league_id),
    sport_id      INT         NOT NULL REFERENCES sport (sport_id),
    venue_id      INT         NOT NULL REFERENCES venue (venue_id),
    match_time    timestamptz NOT NULL,
    host_team_id  INT         NOT NULL REFERENCES team (team_id),
    guest_team_id INT         NOT NULL REFERENCES team (team_id),
    payment_open  BOOLEAN     NOT NULL DEFAULT TRUE,
    CHECK (host_team_id <> guest_team_id)
);

-- ---------------------------------------------------------
-- ticket_config depends on match and ticket_category
-- ---------------------------------------------------------

CREATE TABLE ticket_config
(
    config_id   SERIAL PRIMARY KEY,
    match_id    bigint         NOT NULL REFERENCES "match" (match_id),
    category_id INT            NOT NULL REFERENCES ticket_category (category_id),
    price       numeric(10, 2) NOT NULL CHECK (price >= 0),
    amenities   JSONB,
    total_seats int4           NOT NULL,
    UNIQUE (match_id, category_id)
);

-- ---------------------------------------------------------
-- seat depends on ticket_config
-- ---------------------------------------------------------

CREATE TABLE seat
(
    seat_id   BIGSERIAL PRIMARY KEY,
    config_id INT NOT NULL REFERENCES ticket_config (config_id),
    section   INT NOT NULL,
    row_no    INT NOT NULL,
    seat_no   INT NOT NULL,
    UNIQUE (config_id, section, row_no, seat_no)
);

-- =========================================================
-- Seed data
-- =========================================================

-- ---------------------------------------------------------
-- city
-- ---------------------------------------------------------
INSERT INTO city (name)
VALUES ('Tehran'),
       ('Isfahan'),
       ('Mashhad'),
       ('Shiraz'),
       ('Tabriz');

-- ---------------------------------------------------------
-- sport
-- ---------------------------------------------------------
INSERT INTO sport (sport_name)
VALUES ('Football'),
       ('Basketball'),
       ('Volleyball');

-- ---------------------------------------------------------
-- ticket_category
-- ---------------------------------------------------------
INSERT INTO ticket_category (name)
VALUES ('Regular'),
       ('VIP'),
       ('VVIP'),
       ('Student');

-- ---------------------------------------------------------
-- team
-- ---------------------------------------------------------
INSERT INTO team (name, sport_id, city_id)
VALUES ('Persepolis', (SELECT sport_id FROM sport WHERE sport_name = 'Football'),
        (SELECT city_id FROM city WHERE name = 'Tehran')),
       ('Esteghlal', (SELECT sport_id FROM sport WHERE sport_name = 'Football'),
        (SELECT city_id FROM city WHERE name = 'Tehran')),
       ('Sepahan', (SELECT sport_id FROM sport WHERE sport_name = 'Football'),
        (SELECT city_id FROM city WHERE name = 'Isfahan')),
       ('Tractor', (SELECT sport_id FROM sport WHERE sport_name = 'Football'),
        (SELECT city_id FROM city WHERE name = 'Tabriz')),
       ('Mahram', (SELECT sport_id FROM sport WHERE sport_name = 'Basketball'),
        (SELECT city_id FROM city WHERE name = 'Tehran')),
       ('Foolad', (SELECT sport_id FROM sport WHERE sport_name = 'Volleyball'),
        (SELECT city_id FROM city WHERE name = 'Mashhad'));

-- ---------------------------------------------------------
-- venue
-- ---------------------------------------------------------
INSERT INTO venue (name, city_id, street, postal_code, latitude, longitude)
VALUES ('Azadi Stadium', (SELECT city_id FROM city WHERE name = 'Tehran'), 'Azadi Sports Complex', '1387683411',
        35.7219, 51.3757),
       ('Naghsh-e-Jahan Stadium', (SELECT city_id FROM city WHERE name = 'Isfahan'), 'Naghsh-e-Jahan Blvd',
        '8158683411', 32.6546, 51.6680),
       ('Yadegar-e-Emam Stadium', (SELECT city_id FROM city WHERE name = 'Tabriz'), 'Valiasr Blvd', '5136683411',
        38.0561, 46.2919);

-- ---------------------------------------------------------
-- league
-- ---------------------------------------------------------
INSERT INTO league (name, sport_id)
VALUES ('Persian Gulf Pro League', (SELECT sport_id FROM sport WHERE sport_name = 'Football')),
       ('Basketball Super League', (SELECT sport_id FROM sport WHERE sport_name = 'Basketball')),
       ('Volleyball Super League', (SELECT sport_id FROM sport WHERE sport_name = 'Volleyball'));

-- ---------------------------------------------------------
-- match
-- ---------------------------------------------------------
INSERT INTO "match" (league_id, sport_id, venue_id, match_time, host_team_id, guest_team_id, payment_open)
VALUES ((SELECT league_id FROM league WHERE name = 'Persian Gulf Pro League'),
        (SELECT sport_id FROM sport WHERE sport_name = 'Football'),
        (SELECT venue_id FROM venue WHERE name = 'Azadi Stadium'),
        '2026-08-15 18:00:00+03:30',
        (SELECT team_id FROM team WHERE name = 'Persepolis'),
        (SELECT team_id FROM team WHERE name = 'Esteghlal'),
        TRUE),
       ((SELECT league_id FROM league WHERE name = 'Persian Gulf Pro League'),
        (SELECT sport_id FROM sport WHERE sport_name = 'Football'),
        (SELECT venue_id FROM venue WHERE name = 'Naghsh-e-Jahan Stadium'),
        '2026-08-20 19:00:00+03:30',
        (SELECT team_id FROM team WHERE name = 'Sepahan'),
        (SELECT team_id FROM team WHERE name = 'Tractor'),
        TRUE);

-- ---------------------------------------------------------
-- ticket_config
-- ---------------------------------------------------------
INSERT INTO ticket_config (match_id, category_id, price, amenities, total_seats)
VALUES ((SELECT match_id
         FROM "match"
         WHERE host_team_id = (SELECT team_id FROM team WHERE name = 'Persepolis')
           AND guest_team_id = (SELECT team_id FROM team WHERE name = 'Esteghlal')),
        (SELECT category_id FROM ticket_category WHERE name = 'Regular'),
        250000.00,
        '{
          "cover": false,
          "cushioned_seat": false
        }',
        1000),
       ((SELECT match_id
         FROM "match"
         WHERE host_team_id = (SELECT team_id FROM team WHERE name = 'Persepolis')
           AND guest_team_id = (SELECT team_id FROM team WHERE name = 'Esteghlal')),
        (SELECT category_id FROM ticket_category WHERE name = 'VIP'),
        750000.00,
        '{
          "cover": true,
          "cushioned_seat": true,
          "complimentary_drink": true
        }',
        200),
       ((SELECT match_id
         FROM "match"
         WHERE host_team_id = (SELECT team_id FROM team WHERE name = 'Sepahan')
           AND guest_team_id = (SELECT team_id FROM team WHERE name = 'Tractor')),
        (SELECT category_id FROM ticket_category WHERE name = 'Regular'),
        200000.00,
        '{
          "cover": false,
          "cushioned_seat": false
        }',
        800);

-- ---------------------------------------------------------
-- seat
-- Generates a small block of seats (2 sections x 2 rows x 5 seats)
-- for each ticket_config row created above.
-- ---------------------------------------------------------
INSERT INTO seat (config_id, section, row_no, seat_no)
SELECT tc.config_id, s.section, r.row_no, n.seat_no
FROM ticket_config tc
         CROSS JOIN generate_series(1, 2) AS s(section)
         CROSS JOIN generate_series(1, 2) AS r(row_no)
         CROSS JOIN generate_series(1, 5) AS n(seat_no);