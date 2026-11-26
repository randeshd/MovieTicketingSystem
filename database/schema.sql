-- =====================================================================
--  ABC Cinema - Movie Ticketing System
--  schema.sql : creates the database, all tables, and the starting data
--               the app cannot run without (40 seats, the Half ticket
--               setting, and the first manager account).
--
--  Matches ER-diagram-v2 and Class-diagram-v2.
--  Needs MySQL 8.0.16 or newer (CHECK constraints are enforced from 8.0.16).
--
--  SAFE TO RERUN: it drops the whole database first, so running it again
--  gives you a clean, empty system. Run sample-data.sql after it if you
--  want demo movies, showtimes and bookings.
-- =====================================================================

DROP DATABASE IF EXISTS movie_ticketing;

-- utf8mb4 stores every character (Sinhala, Tamil, emoji) correctly.
CREATE DATABASE movie_ticketing
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE movie_ticketing;


-- ---------------------------------------------------------------------
-- 1. users  (the "User" superclass in the ER ISA)
--    Every person who logs in has exactly one row here.
--    role tells Java which subclass to build after login
--    (Customer, Cashier or Manager), so login needs only one query.
-- ---------------------------------------------------------------------
CREATE TABLE users (
  user_id        INT           NOT NULL AUTO_INCREMENT,
  name           VARCHAR(100)  NOT NULL,
  email          VARCHAR(150)  NOT NULL,
  -- Salted SHA-256, stored as  base64(salt) + ':' + base64(hash).
  -- Never the plain password (R29).
  password_hash  VARCHAR(100)  NOT NULL,
  role           ENUM('CUSTOMER','CASHIER','MANAGER') NOT NULL,
  -- Accounts are deactivated, never deleted (R31).
  is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (user_id),
  CONSTRAINT uq_users_email UNIQUE (email)          -- R29: email is unique
);


-- ---------------------------------------------------------------------
-- 2-4. customers, cashiers, managers  (the ISA subclasses)
--    Each subclass table has its own id plus a link to users.
--    user_id is UNIQUE, so one user can be at most one customer.
--    Other tables point at the subclass id (customer_id, cashier_id,
--    manager_id) so the database itself guarantees, for example,
--    that only a cashier can print a ticket.
-- ---------------------------------------------------------------------
CREATE TABLE customers (
  customer_id    INT           NOT NULL AUTO_INCREMENT,
  user_id        INT           NOT NULL,
  nic            VARCHAR(12)   NOT NULL,   -- old 9 digits + V, or new 12 digits
  phone          VARCHAR(15)   NOT NULL,
  date_of_birth  DATE          NOT NULL,   -- for the 18+ check (R19)

  PRIMARY KEY (customer_id),
  CONSTRAINT uq_customers_user UNIQUE (user_id),
  CONSTRAINT uq_customers_nic  UNIQUE (nic),
  CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE cashiers (
  cashier_id     INT           NOT NULL AUTO_INCREMENT,
  user_id        INT           NOT NULL,

  PRIMARY KEY (cashier_id),
  CONSTRAINT uq_cashiers_user UNIQUE (user_id),
  CONSTRAINT fk_cashiers_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE managers (
  manager_id     INT           NOT NULL AUTO_INCREMENT,
  user_id        INT           NOT NULL,

  PRIMARY KEY (manager_id),
  CONSTRAINT uq_managers_user UNIQUE (user_id),
  CONSTRAINT fk_managers_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);


-- ---------------------------------------------------------------------
-- 5. movies   (Manager --Manages--> Movie)
--    A movie that has showtimes is never deleted, only ARCHIVED (R06).
--    The foreign key from showtimes blocks the delete anyway.
-- ---------------------------------------------------------------------
CREATE TABLE movies (
  movie_id       INT           NOT NULL AUTO_INCREMENT,
  title          VARCHAR(150)  NOT NULL,
  genre          VARCHAR(50)   NOT NULL,
  description    TEXT          NULL,
  duration_min   SMALLINT      NOT NULL,
  is_18_plus     BOOLEAN       NOT NULL DEFAULT FALSE,   -- R04
  trailer_url    VARCHAR(255)  NULL,
  base_price     DECIMAL(8,2)  NOT NULL,                 -- Full ticket price, Rs.
  status         ENUM('COMING_SOON','NOW_SHOWING','ARCHIVED')
                               NOT NULL DEFAULT 'COMING_SOON',   -- R05
  created_by     INT           NOT NULL,                 -- manager_id

  PRIMARY KEY (movie_id),
  CONSTRAINT chk_movies_duration CHECK (duration_min BETWEEN 1 AND 600),
  CONSTRAINT chk_movies_price    CHECK (base_price > 0),
  CONSTRAINT fk_movies_manager   FOREIGN KEY (created_by) REFERENCES managers (manager_id)
);


-- ---------------------------------------------------------------------
-- 6. showtimes   (Movie --Scheduled--> Showtime, Manager --Schedules-->)
--    One hall, so two shows can never start at the same date and time.
--    The full "no overlap + 15 minute cleaning gap" rule (R08) needs the
--    movie length, so Java checks it (Showtime.overlaps()).
--    end_time can be after midnight (e.g. 22:30 + 2h = 00:30), which is
--    why there is no "end_time > show_time" check.
-- ---------------------------------------------------------------------
CREATE TABLE showtimes (
  showtime_id    INT           NOT NULL AUTO_INCREMENT,
  movie_id       INT           NOT NULL,
  show_date      DATE          NOT NULL,
  show_time      TIME          NOT NULL,
  end_time       TIME          NOT NULL,   -- start + movie duration
  created_by     INT           NOT NULL,   -- manager_id

  PRIMARY KEY (showtime_id),
  CONSTRAINT uq_showtimes_slot  UNIQUE (show_date, show_time),
  CONSTRAINT chk_showtimes_hours CHECK (show_time BETWEEN '09:00:00' AND '22:30:00'),  -- R07
  CONSTRAINT fk_showtimes_movie   FOREIGN KEY (movie_id)   REFERENCES movies (movie_id),
  CONSTRAINT fk_showtimes_manager FOREIGN KEY (created_by) REFERENCES managers (manager_id)
);


-- ---------------------------------------------------------------------
-- 7. seats   (one hall: rows A to E, seats 1 to 8 = 40 seats, R01)
--    Row A is nearest the screen. Label in Java: row_letter + seat_number, e.g. C5.
-- ---------------------------------------------------------------------
CREATE TABLE seats (
  seat_id        INT           NOT NULL AUTO_INCREMENT,
  row_letter     CHAR(1)       NOT NULL,
  seat_number    TINYINT       NOT NULL,

  PRIMARY KEY (seat_id),
  CONSTRAINT uq_seats_position UNIQUE (row_letter, seat_number),
  CONSTRAINT chk_seats_row     CHECK (row_letter IN ('A','B','C','D','E')),
  CONSTRAINT chk_seats_number  CHECK (seat_number BETWEEN 1 AND 8)
);


-- ---------------------------------------------------------------------
-- 8. bookings   (Customer --Makes--> Booking, Cashier --Processes--> Booking)
--    Online booking : customer_id is set, cashier_id is empty.
--    Walk-in sale   : cashier_id is set, customer_id is empty
--                     (walk-in customers have no account).
--    Life cycle (R17, R27):
--      HELD       seats are held; hold_expires_at = booking_time + 5 minutes
--      CONFIRMED  paid (a payments row exists)
--      EXPIRED    not paid in time; its tickets were deleted, seats are free
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
  booking_id        INT           NOT NULL AUTO_INCREMENT,
  booking_reference CHAR(9)       NOT NULL,   -- e.g. BK-7F3K2Q (R18)
  customer_id       INT           NULL,
  cashier_id        INT           NULL,
  showtime_id       INT           NOT NULL,
  status            ENUM('HELD','CONFIRMED','EXPIRED') NOT NULL DEFAULT 'HELD',
  booking_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  hold_expires_at   DATETIME      NULL,
  total_amount      DECIMAL(10,2) NOT NULL,

  PRIMARY KEY (booking_id),
  CONSTRAINT uq_bookings_reference UNIQUE (booking_reference),
  -- Exactly one of customer_id / cashier_id is filled in.
  CONSTRAINT chk_bookings_who   CHECK ((customer_id IS NULL) <> (cashier_id IS NULL)),
  -- A held booking must know when its hold ends.
  CONSTRAINT chk_bookings_hold  CHECK (status <> 'HELD' OR hold_expires_at IS NOT NULL),
  CONSTRAINT chk_bookings_total CHECK (total_amount >= 0),
  CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
  CONSTRAINT fk_bookings_cashier  FOREIGN KEY (cashier_id)  REFERENCES cashiers (cashier_id),
  CONSTRAINT fk_bookings_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (showtime_id)
);


-- ---------------------------------------------------------------------
-- 9. tickets   (Booking --Contains--> Ticket, Seat --Has--> Ticket,
--               Cashier --Prints--> Ticket)
--    One row per seat in a booking (R14, R23).
--    UNIQUE (showtime_id, seat_id) is the safety net for R15 and R28:
--    if two people try to buy the same seat for the same show at the
--    same moment, MySQL accepts the first INSERT and rejects the second
--    with error 1062 (duplicate entry). Java turns that error into a
--    SeatUnavailableException.
--    Held and paid tickets both exist as rows, so both count as taken.
--    price is the price actually charged, so later price changes never
--    rewrite old sales (R13).
-- ---------------------------------------------------------------------
CREATE TABLE tickets (
  ticket_id      INT           NOT NULL AUTO_INCREMENT,
  booking_id     INT           NOT NULL,
  showtime_id    INT           NOT NULL,
  seat_id        INT           NOT NULL,
  ticket_type    ENUM('FULL','HALF') NOT NULL DEFAULT 'FULL',   -- R10
  price          DECIMAL(8,2)  NOT NULL,
  printed_at     DATETIME      NULL,       -- empty until printed (R24)
  printed_by     INT           NULL,       -- cashier_id who printed it

  PRIMARY KEY (ticket_id),
  CONSTRAINT uq_tickets_seat_per_show UNIQUE (showtime_id, seat_id),
  CONSTRAINT chk_tickets_price   CHECK (price > 0),
  -- printed_at and printed_by are either both empty or both filled in.
  CONSTRAINT chk_tickets_printed CHECK ((printed_at IS NULL) = (printed_by IS NULL)),
  -- Deleting a booking deletes its tickets too (used for expired holds).
  CONSTRAINT fk_tickets_booking  FOREIGN KEY (booking_id)  REFERENCES bookings (booking_id)
                                 ON DELETE CASCADE,
  CONSTRAINT fk_tickets_showtime FOREIGN KEY (showtime_id) REFERENCES showtimes (showtime_id),
  CONSTRAINT fk_tickets_seat     FOREIGN KEY (seat_id)     REFERENCES seats (seat_id),
  CONSTRAINT fk_tickets_cashier  FOREIGN KEY (printed_by)  REFERENCES cashiers (cashier_id)
);


-- ---------------------------------------------------------------------
-- 10. payments   (Booking --PaidVia--> Payment)
--    ONE table for both Java subclasses (CardPayment, CashPayment).
--    PaymentDAO reads payment_method and builds the right object.
--    Columns that belong to only one subclass are empty for the other:
--      CARD : card_last4 (online, only last 4 digits, R20), at_counter
--      CASH : amount_received, change_given (R21), always at the counter
--    One payment per booking (R22), so booking_id is UNIQUE.
--    Only successful payments are saved in normal use; FAILED exists so
--    the PaymentStatus enum has a matching value.
-- ---------------------------------------------------------------------
CREATE TABLE payments (
  payment_id      INT           NOT NULL AUTO_INCREMENT,
  booking_id      INT           NOT NULL,
  payment_method  ENUM('CARD','CASH') NOT NULL,
  at_counter      BOOLEAN       NOT NULL,
  amount          DECIMAL(10,2) NOT NULL,
  status          ENUM('SUCCESS','FAILED') NOT NULL DEFAULT 'SUCCESS',
  payment_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  card_last4      CHAR(4)       NULL,
  amount_received DECIMAL(10,2) NULL,
  change_given    DECIMAL(10,2) NULL,

  PRIMARY KEY (payment_id),
  CONSTRAINT uq_payments_booking UNIQUE (booking_id),
  CONSTRAINT chk_payments_amount CHECK (amount > 0),
  -- Cash: at the counter, received covers the total, change is the difference.
  -- Card: no cash columns; online card payments must store the last 4 digits.
  CONSTRAINT chk_payments_method CHECK (
       (payment_method = 'CASH'
          AND at_counter = TRUE
          AND card_last4 IS NULL
          AND amount_received >= amount
          AND change_given = amount_received - amount)
    OR (payment_method = 'CARD'
          AND amount_received IS NULL
          AND change_given IS NULL
          AND (at_counter = TRUE OR card_last4 IS NOT NULL))
  ),
  CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (booking_id)
);


-- ---------------------------------------------------------------------
-- 11. settings   (Manager --Updates--> Setting)
--    Key/value pairs a manager can change without touching code.
--    Values are stored as text; Setting.getValueAsDouble() converts.
-- ---------------------------------------------------------------------
CREATE TABLE settings (
  setting_key    VARCHAR(50)   NOT NULL,
  setting_value  VARCHAR(100)  NOT NULL,
  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by     INT           NULL,       -- manager_id, empty for the default value

  PRIMARY KEY (setting_key),
  CONSTRAINT fk_settings_manager FOREIGN KEY (updated_by) REFERENCES managers (manager_id)
);


-- =====================================================================
--  STARTING DATA  (the app needs these rows even with no demo data)
-- =====================================================================

-- The 40 seats: every row A-E crossed with every number 1-8.
INSERT INTO seats (row_letter, seat_number)
SELECT r.row_letter, n.seat_number
FROM       (SELECT 'A' AS row_letter UNION ALL SELECT 'B' UNION ALL SELECT 'C'
            UNION ALL SELECT 'D' UNION ALL SELECT 'E') AS r
CROSS JOIN (SELECT 1 AS seat_number UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8) AS n
ORDER BY r.row_letter, n.seat_number;
-- Result: A1 = seat_id 1 ... A8 = 8, B1 = 9 ... E8 = 40.

-- Half ticket = 50% of the base price by default (R10). A manager can change it.
INSERT INTO settings (setting_key, setting_value) VALUES ('HALF_TICKET_PERCENT', '50');

-- The first manager is loaded with the database (business case, "Who uses it").
-- Login: manager@abccinema.lk / Password@123   (change it after the first login)
INSERT INTO users (user_id, name, email, password_hash, role) VALUES
  (1, 'Cinema Manager', 'manager@abccinema.lk',
   'MAk8+gAF8A+i7RXRI8Oz+A==:tHEp57bsmOj4Bl1SnKfFHmzagtYygat4cSmPcsYdXjU=', 'MANAGER');
INSERT INTO managers (manager_id, user_id) VALUES (1, 1);
