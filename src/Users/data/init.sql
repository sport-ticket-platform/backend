-- ============================================
-- ENUM TYPES
-- ============================================

CREATE TYPE user_role AS ENUM (
    'USER',
    'ADMIN',
    'SUPPORT'
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


-- ============================================
-- CITY TABLE
-- ============================================

CREATE TABLE city
(
    city_id SERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE
);


-- ============================================
-- USERS TABLE
-- ============================================

CREATE TABLE users
(
    user_id            BIGSERIAL PRIMARY KEY,

    first_name         VARCHAR(50)           NOT NULL,
    last_name          VARCHAR(50)           NOT NULL,

    role               user_role             NOT NULL DEFAULT 'USER',

    email              VARCHAR(255)          NOT NULL UNIQUE,
    email_verified     BOOLEAN DEFAULT FALSE NOT NULL,

    phone_number       VARCHAR(20) UNIQUE,
    phone_verified     BOOLEAN DEFAULT FALSE NOT NULL,

    registration_date  TIMESTAMPTZ
                               DEFAULT CURRENT_TIMESTAMP
                                             NOT NULL,

    password           VARCHAR(255)          NOT NULL,

    balance            NUMERIC(12, 2)
                               DEFAULT 0
                                             NOT NULL,

    city_id            INT
        REFERENCES city (city_id),

    is_active          BOOLEAN
                               DEFAULT TRUE
                                             NOT NULL,

    two_factor_enabled BOOLEAN
                               DEFAULT FALSE
                                             NOT NULL
);


-- ============================================
-- REPORT TABLE
-- ============================================

CREATE TABLE report
(
    report_id    BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL
        REFERENCES users (user_id),

    type         report_type   NOT NULL,

    reported_at  TIMESTAMPTZ
        DEFAULT CURRENT_TIMESTAMP
                               NOT NULL,

    request      VARCHAR(500)  NOT NULL,
    response     VARCHAR(500),
    responded_at TIMESTAMPTZ,

    status       report_status NOT NULL
);


-- ============================================
-- DEFAULT CITIES
-- ============================================

INSERT INTO city (name)
VALUES ('Berlin'),
       ('Munich'),
       ('Hamburg'),
       ('Frankfurt'),
       ('Cologne'),
       ('Nuremberg'),
       ('Stuttgart');


-- ============================================
-- DEFAULT USERS
-- ============================================

INSERT INTO users (first_name,
                   last_name,
                   role,
                   email,
                   email_verified,
                   phone_number,
                   phone_verified,
                   password,
                   balance,
                   city_id,
                   is_active,
                   two_factor_enabled)
VALUES ('John',
        'Doe',
        'USER',
        'john.doe@example.com',
        TRUE,
        '09360110494',
        TRUE,
        '$2a$11$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        100.00,
        (SELECT city_id FROM city WHERE name = 'Berlin'),
        TRUE,
        FALSE),

       ('Alice',
        'Smith',
        'USER',
        'alice.smith@example.com',
        TRUE,
        '09360110498',
        TRUE,
        '$2a$11$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        250.50,
        (SELECT city_id FROM city WHERE name = 'Munich'),
        TRUE,
        TRUE),

       ('Admin',
        'User',
        'ADMIN',
        'admin@example.com',
        TRUE,
        '09360110499',
        TRUE,
        '$2a$11$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        0.00,
        (SELECT city_id FROM city WHERE name = 'Nuremberg'),
        TRUE,
        TRUE),

       ('Support',
        'Agent',
        'USER',
        'support@example.com',
        TRUE,
        '09360110490',
        TRUE,
        '$2a$11$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        0.00,
        (SELECT city_id FROM city WHERE name = 'Hamburg'),
        TRUE,
        FALSE);


-- ============================================
-- DEFAULT REPORTS
-- ============================================

INSERT INTO report (user_id,
                    type,
                    request,
                    response,
                    responded_at,
                    status)
VALUES ((SELECT user_id FROM users WHERE email = 'john.doe@example.com'),
        'PAYMENT_ISSUE',
        'I was charged twice for the same ticket order, please refund the extra charge.',
        NULL,
        NULL,
        'OPEN'),

       ((SELECT user_id FROM users WHERE email = 'alice.smith@example.com'),
        'RESERVATION_ISSUE',
        'My seat reservation expired before I could complete checkout, even though I was still within the time limit.',
        'We have extended your reservation window and applied a small credit to your account for the inconvenience.',
        CURRENT_TIMESTAMP,
        'CLOSED'),

       ((SELECT user_id FROM users WHERE email = 'john.doe@example.com'),
        'TECHNICAL_BUG',
        'The seat map does not load on mobile Safari, seats appear blank and cannot be selected.',
        NULL,
        NULL,
        'IN_PROGRESS');