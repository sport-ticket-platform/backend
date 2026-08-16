-- ==========================================
-- 1. SCHEMA CREATION (DDL)
-- ==========================================

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

CREATE TYPE user_role AS ENUM (
    'USER',
    'ADMIN',
    'SUPPORT'
);

CREATE TABLE users
(
    user_id            BIGSERIAL PRIMARY KEY,
    first_name         varchar(50)                           NOT NULL,
    last_name          varchar(50)                           NOT NULL,
    role               user_role                             NOT NULL DEFAULT 'USER',
    email              varchar(255)                          NOT NULL UNIQUE,
    email_verified     bool        DEFAULT false             NOT NULL,
    phone_number       varchar(20) UNIQUE,
    phone_verified     bool        DEFAULT false             NOT NULL,
    registration_date  timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    password           varchar(255)                          NOT NULL,
    city_id            INT REFERENCES city (city_id),
    is_active          bool        DEFAULT true              NOT NULL,
    two_factor_enabled bool        DEFAULT false             NOT NULL
);

CREATE TABLE wallet
(
    wallet_id BIGSERIAL PRIMARY KEY,
    user_id   BIGINT UNIQUE NOT NULL REFERENCES users (user_id),
    balance   NUMERIC(12, 2)         DEFAULT 0 NOT NULL CHECK (balance >= 0),
    is_active BOOLEAN       NOT NULL DEFAULT true
);

CREATE TYPE transaction_type AS ENUM (
    'DEPOSIT',
    'WITHDRAWAL'
);

CREATE TYPE transaction_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED'
);

CREATE TYPE transaction_reference_type AS ENUM (
    'PAYMENT',
    'TICKET_ORDER',
    'SUPPORT_REPORT',
    'SYSTEM_ADJUSTMENT'
);

CREATE TABLE wallet_transaction
(
    transaction_id BIGSERIAL PRIMARY KEY,
    wallet_id      BIGINT             NOT NULL REFERENCES wallet (wallet_id),
    type           transaction_type   NOT NULL,
    status         transaction_status NOT NULL DEFAULT 'PENDING',
    amount         NUMERIC(12, 2)     NOT NULL CHECK (amount > 0),
    balance_after  NUMERIC(12, 2),
    description    VARCHAR(255),
    reference_type transaction_reference_type,
    reference_id   BIGINT,
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    latitude    decimal      Not Null,
    longitude   decimal      Not Null
);

CREATE TABLE league
(
    league_id SERIAL PRIMARY KEY,
    name      varchar(50) NOT NULL,
    sport_id  int4        NOT NULL REFERENCES sport (sport_id),
    UNIQUE (sport_id, name)
);

CREATE TABLE "match"
(
    match_id        BIGSERIAL PRIMARY KEY,
    league_id       INT         NOT NULL REFERENCES league (league_id),
    sport_id        INT         NOT NULL REFERENCES sport (sport_id),
    venue_id        INT         NOT NULL REFERENCES venue (venue_id),
    match_time      timestamptz NOT NULL,
    host_team_id    INT         NOT NULL REFERENCES team (team_id),
    guest_team_id   INT         NOT NULL REFERENCES team (team_id),
    is_payment_open BOOLEAN     NOT NULL DEFAULT TRUE,
    CHECK (host_team_id <> guest_team_id)
);

CREATE TABLE ticket_category
(
    category_id SERIAL PRIMARY KEY,
    name        varchar(50) NOT NULL
);

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

CREATE TABLE seat
(
    seat_id   BIGSERIAL PRIMARY KEY,
    config_id INT NOT NULL REFERENCES ticket_config (config_id),
    section   INT NOT NULL,
    row_no    INT NOT NULL,
    seat_no   INT NOT NULL,
    UNIQUE (config_id, section, row_no, seat_no)
);

CREATE TYPE reservation_status AS ENUM (
    'ACTIVE',
    'EXPIRED',
    'COMPLETED',
    'CANCELLED'
);

CREATE TABLE reservation
(
    reservation_id BIGSERIAL PRIMARY KEY,
    user_id        BIGINT             NOT NULL REFERENCES users (user_id),
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ        NOT NULL,
    status         reservation_status NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE reservation_seat
(
    reservation_id BIGINT  NOT NULL REFERENCES reservation (reservation_id),
    config_id      int     NOT NULL REFERENCES ticket_config (config_id),
    seat_id        BIGINT  NOT NULL REFERENCES seat (seat_id),
    is_active      BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (reservation_id, seat_id)
);

CREATE UNIQUE INDEX uq_seat_active_once
    ON reservation_seat (seat_id) WHERE is_active;

CREATE TYPE order_status AS ENUM (
    'PENDING',
    'PAID',
    'FAILED',
    'REFUNDED'
);

CREATE TABLE ticket_order
(
    order_id       BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT UNIQUE REFERENCES reservation (reservation_id),
    user_id        BIGINT         NOT NULL REFERENCES users (user_id),
    total_amount   NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    status         order_status   NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE payment_methods
(
    method_id      SERIAL PRIMARY KEY,
    name           varchar(50) NOT NULL,
    fee_percentage numeric(5, 2),
    is_active      boolean DEFAULT true
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'SUCCEEDED',
    'FAILED',
    'REFUNDED'
);

CREATE TABLE payment
(
    payment_id BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES ticket_order (order_id),
    method_id  INT            NOT NULL REFERENCES payment_methods (method_id),
    amount     NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    token      VARCHAR(255) UNIQUE,
    ref_id     VARCHAR(100) UNIQUE,
    paid_at    TIMESTAMPTZ,
    status     payment_status NOT NULL DEFAULT 'PENDING'
);

CREATE TYPE ticket_status AS ENUM (
    'VALID',
    'CANCELED_BY_USER',
    'CANCELED_BY_SYSTEM',
    'REFUNDED'
);

CREATE TABLE sold_ticket
(
    ticket_id  BIGSERIAL PRIMARY KEY,
    seat_id    BIGINT         NOT NULL REFERENCES seat (seat_id),
    order_id   BIGINT         NOT NULL REFERENCES ticket_order (order_id),
    price      NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    status     ticket_status  NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TYPE report_type AS ENUM (
    'PAYMENT_ISSUE',
    'RESERVATION_ISSUE',
    'CANCEL_RESERVATION',
    'TECHNICAL_BUG',
    'COMPLAINT',
    'OTHER'
);

CREATE TYPE report_status AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'CLOSED'
);

CREATE TABLE report
(
    report_id    BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users (user_id),
    type         report_type   NOT NULL,
    reported_at  timestamptz   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request      varchar(500)  NOT NULL,
    response     varchar(500),
    responded_at timestamptz,
    status       report_status NOT NULL
);

CREATE TABLE refresh_token
(
    token_id        BIGSERIAL PRIMARY KEY,
    token           varchar(255) NOT NULL UNIQUE,
    user_id         BIGINT       NOT NULL REFERENCES users (user_id),
    created_at      timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiration_date timestamptz  NOT NULL,
    is_active       bool         NOT NULL DEFAULT true,
    revoked_at      timestamptz,
    revoked_reason  text,
    ip_address      varchar(45),
    user_agent      text,
    device_id       varchar(255)
);

CREATE INDEX idx_refresh_token_user_id
    ON refresh_token (user_id);

CREATE INDEX idx_refresh_token_is_active
    ON refresh_token (is_active);

CREATE INDEX idx_refresh_token_device_id
    ON refresh_token (device_id);

CREATE TABLE app_setting
(
    id                              BIGINT PRIMARY KEY DEFAULT 1,
    max_failed_login_attempts       INT     NOT NULL
        CHECK (max_failed_login_attempts >= 1)
                                                       DEFAULT 5,
    account_lockout_duration_second INT     NOT NULL
        CHECK (account_lockout_duration_second >= 60)
                                                       DEFAULT 600,
    allow_concurrent_logins         BOOLEAN NOT NULL   DEFAULT true,
    maintenance_mode                BOOLEAN NOT NULL   DEFAULT false,
    allow_new_registrations         BOOLEAN NOT NULL   DEFAULT true,
    allow_login                     BOOLEAN NOT NULL   DEFAULT true,
    CHECK (id = 1)
);

-- ==========================================
-- 2. SAMPLE DATA INSERTION (DML)
-- ==========================================

INSERT INTO city (city_id, name)
VALUES (1, 'New York'),
       (2, 'Los Angeles'),
       (3, 'London'),
       (4, 'Paris'),
       (5, 'Tokyo');

INSERT INTO sport (sport_id, sport_name)
VALUES (1, 'Football'),
       (2, 'Basketball'),
       (3, 'Tennis'),
       (4, 'Baseball');

-- Passwords represent hashed versions (e.g. bcrypt/argon2) for realism
INSERT INTO users (user_id, first_name, last_name, role, email, email_verified, phone_number, phone_verified, password,
                   city_id, is_active, two_factor_enabled)
VALUES (1, 'Admin', 'User', 'ADMIN', 'admin@ticketmaster.local', true, '+1000000000', true,
        '$2a$10$xyzHashedStringForAdmin', 1, true, true),
       (2, 'John', 'Doe', 'USER', 'johndoe@example.com', true, '+12125551234', true, '$2a$10$xyzHashedStringForJohn', 1,
        true, false),
       (3, 'Jane', 'Smith', 'USER', 'janesmith@example.com', true, '+13235559876', false,
        '$2a$10$xyzHashedStringForJane', 2, true, false),
       (4, 'Alice', 'Johnson', 'USER', 'alice.j@example.co.uk', true, '+442079460958', true,
        '$2a$10$xyzHashedStringForAlice', 3, true, true),
       (5, 'Support', 'Staff', 'SUPPORT', 'support@ticketmaster.local', true, '+1000000001', true,
        '$2a$10$xyzHashedStringForSupport', 1, true, true);

INSERT INTO wallet (wallet_id, user_id, balance, is_active)
VALUES (1, 2, 250.00, true),
       (2, 3, 50.00, true),
       (3, 4, 1000.00, true);

INSERT INTO wallet_transaction (transaction_id, wallet_id, type, status, amount, balance_after, description,
                                reference_type, reference_id)
VALUES (1, 1, 'DEPOSIT', 'SUCCESS', 300.00, 300.00, 'Initial Deposit', 'SYSTEM_ADJUSTMENT', NULL),
       (2, 1, 'WITHDRAWAL', 'SUCCESS', 50.00, 250.00, 'Purchased Standard Ticket', 'TICKET_ORDER', 1),
       (3, 3, 'DEPOSIT', 'SUCCESS', 1000.00, 1000.00, 'Crypto Deposit', 'SYSTEM_ADJUSTMENT', NULL);

INSERT INTO team (team_id, name, sport_id, city_id)
VALUES (1, 'New York City FC', 1, 1),
       (2, 'LA Galaxy', 1, 2),
       (3, 'NY Knicks', 2, 1),
       (4, 'LA Lakers', 2, 2),
       (5, 'Arsenal', 1, 3),
       (6, 'Chelsea', 1, 3);

INSERT INTO venue (venue_id, name, city_id, street, postal_code, latitude, longitude)
VALUES (1, 'Yankee Stadium', 1, '1 E 161 St', '10451', 40.829, -73.926),
       (2, 'Staples Center', 2, '1111 S Figueroa St', '90015', 34.043, -118.267),
       (3, 'Emirates Stadium', 3, 'Hornsey Rd', 'N7 7AJ', 51.554, -0.108);

INSERT INTO league (league_id, name, sport_id)
VALUES (1, 'Major League Soccer', 1),
       (2, 'NBA', 2),
       (3, 'Premier League', 1);

INSERT INTO "match" (match_id, league_id, sport_id, venue_id, match_time, host_team_id, guest_team_id, is_payment_open)
VALUES (1, 1, 1, 1, NOW() + INTERVAL '10 days', 1, 2, true),
       (2, 2, 2, 2, NOW() + INTERVAL '15 days', 4, 3, true),
       (3, 3, 1, 3, NOW() + INTERVAL '20 days', 5, 6, true);

INSERT INTO ticket_category (category_id, name)
VALUES (1, 'VIP Box'),
       (2, 'Standard Lower'),
       (3, 'Standard Upper'),
       (4, 'Economy');

INSERT INTO ticket_config (config_id, match_id, category_id, price, amenities, total_seats)
VALUES (1, 1, 1, 250.00, '{
  "food": "buffet",
  "parking": "valet",
  "lounge": true
}', 50),
       (2, 1, 2, 80.00, '{
         "food": "none",
         "view": "pitch level"
       }', 500),
       (3, 2, 1, 500.00, '{
         "food": "premium",
         "parking": "valet",
         "meet_greet": true
       }', 20),
       (4, 2, 3, 120.00, '{
         "food": "none",
         "view": "mid-level"
       }', 300),
       (5, 3, 2, 95.00, '{
         "food": "none"
       }', 1000);

-- Generating a small block of seats mapped to config items
INSERT INTO seat (seat_id, config_id, section, row_no, seat_no)
VALUES (1, 1, 1, 1, 1),
       (2, 1, 1, 1, 2),
       (3, 1, 1, 1, 3),
       (4, 1, 1, 1, 4),
       (5, 2, 10, 1, 1),
       (6, 2, 10, 1, 2),
       (7, 2, 10, 1, 3),
       (8, 2, 10, 1, 4),
       (9, 3, 1, 1, 1),
       (10, 3, 1, 1, 2),
       (11, 4, 20, 5, 1),
       (12, 4, 20, 5, 2),
       (13, 5, 5, 10, 1),
       (14, 5, 5, 10, 2);

INSERT INTO reservation (reservation_id, user_id, created_at, expires_at, status)
VALUES (1, 2, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '15 minutes', 'COMPLETED'),
       (2, 3, NOW() - INTERVAL '5 minutes', NOW() + INTERVAL '10 minutes', 'ACTIVE'),
       (3, 4, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', 'EXPIRED');

INSERT INTO reservation_seat (reservation_id, config_id, seat_id, is_active)
VALUES (1, 1, 1, false), -- Completed order, reservation itself is no longer taking up "active" queue space
       (1, 1, 2, false),
       (2, 4, 11, true), -- Active reservation waiting to be checked out
       (2, 4, 12, true),
       (3, 2, 5, false); -- Expired reservation

INSERT INTO ticket_order (order_id, reservation_id, user_id, total_amount, status, created_at)
VALUES (1, 1, 2, 500.00, 'PAID', NOW() - INTERVAL '55 minutes');

INSERT INTO payment_methods (method_id, name, fee_percentage, is_active)
VALUES (1, 'Credit Card', 2.50, true),
       (2, 'PayPal', 3.00, true),
       (3, 'Apple Pay', 1.50, true),
       (4, 'Wallet', 0.00, true);

INSERT INTO payment (payment_id, order_id, method_id, amount, token, ref_id, paid_at, status)
VALUES (1, 1, 1, 512.50, 'tok_1J9Klz2eZvKYlo2C8', 'pi_1J9Klz2eZvKYlo2C', NOW() - INTERVAL '54 minutes', 'SUCCEEDED');

INSERT INTO sold_ticket (ticket_id, seat_id, order_id, price, status, created_at, updated_at)
VALUES (1, 1, 1, 250.00, 'VALID', NOW() - INTERVAL '54 minutes', NOW() - INTERVAL '54 minutes'),
       (2, 2, 1, 250.00, 'VALID', NOW() - INTERVAL '54 minutes', NOW() - INTERVAL '54 minutes');

INSERT INTO report (report_id, user_id, type, reported_at, request, response, responded_at, status)
VALUES (1, 3, 'PAYMENT_ISSUE', NOW() - INTERVAL '1 day', 'I tried to use Apple Pay and got an error 500.', NULL, NULL,
        'OPEN'),
       (2, 2, 'COMPLAINT', NOW() - INTERVAL '2 hours', 'The seat view on the app looks different from the website.',
        'We are looking into this UI bug. Thanks!', NOW() - INTERVAL '1 hour', 'IN_PROGRESS');

INSERT INTO refresh_token (token_id, token, user_id, created_at, expiration_date, is_active, revoked_at, revoked_reason,
                           ip_address, user_agent, device_id)
VALUES (1, 'eyJhbGciOiJIUzI1NiIsInR5c...', 2, NOW(), NOW() + INTERVAL '7 days', true, NULL, NULL, '192.168.1.45',
        'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0...)', 'device-abc-123'),
       (2, 'eyJhbGciOiJIUzI1NiIsInR5d...', 4, NOW() - INTERVAL '1 day', NOW() + INTERVAL '6 days', true, NULL, NULL,
        '10.0.0.99', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'device-xyz-987');

INSERT INTO app_setting (id, max_failed_login_attempts, account_lockout_duration_second, allow_concurrent_logins,
                         maintenance_mode, allow_new_registrations, allow_login)
VALUES (1, 5, 600, false, false, true, true);

-- ==========================================
-- 3. SEQUENCE SYNCHRONIZATION
-- ==========================================
-- This step prevents "duplicate key" errors when you insert new records through the application 
-- by making sure the auto-increment sequences know about the explicit IDs we just inserted.

SELECT setval('city_city_id_seq', (SELECT MAX(city_id) FROM city));
SELECT setval('sport_sport_id_seq', (SELECT MAX(sport_id) FROM sport));
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('wallet_wallet_id_seq', (SELECT MAX(wallet_id) FROM wallet));
SELECT setval('wallet_transaction_transaction_id_seq', (SELECT MAX(transaction_id) FROM wallet_transaction));
SELECT setval('team_team_id_seq', (SELECT MAX(team_id) FROM team));
SELECT setval('venue_venue_id_seq', (SELECT MAX(venue_id) FROM venue));
SELECT setval('league_league_id_seq', (SELECT MAX(league_id) FROM league));
SELECT setval('match_match_id_seq', (SELECT MAX(match_id) FROM "match"));
SELECT setval('ticket_category_category_id_seq', (SELECT MAX(category_id) FROM ticket_category));
SELECT setval('ticket_config_config_id_seq', (SELECT MAX(config_id) FROM ticket_config));
SELECT setval('seat_seat_id_seq', (SELECT MAX(seat_id) FROM seat));
SELECT setval('reservation_reservation_id_seq', (SELECT MAX(reservation_id) FROM reservation));
SELECT setval('ticket_order_order_id_seq', (SELECT MAX(order_id) FROM ticket_order));
SELECT setval('payment_methods_method_id_seq', (SELECT MAX(method_id) FROM payment_methods));
SELECT setval('payment_payment_id_seq', (SELECT MAX(payment_id) FROM payment));
SELECT setval('sold_ticket_ticket_id_seq', (SELECT MAX(ticket_id) FROM sold_ticket));
SELECT setval('report_report_id_seq', (SELECT MAX(report_id) FROM report));
SELECT setval('refresh_token_token_id_seq', (SELECT MAX(token_id) FROM refresh_token));