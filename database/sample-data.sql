-- =====================================================================
--  ABC Cinema - Movie Ticketing System
--  sample-data.sql : demo data for testing the app and the reports.
--
--  Run schema.sql FIRST (it creates the database and the 40 seats).
--  To start over, run schema.sql again, then this file again.
--
--  Every total here matches its tickets, every payment matches its
--  booking, and Half tickets are exactly 50% of the base price, so the
--  reports show correct numbers.
--
--  ALL sample logins use the password:  Password@123
--    manager@abccinema.lk     Manager   (created in schema.sql)
--    saman@abccinema.lk       Cashier
--    dilani@abccinema.lk      Cashier
--    ruwan@abccinema.lk       Cashier   (DEACTIVATED: login must fail)
--    amaya@gmail.com          Customer  (adult)
--    kavindu@gmail.com        Customer  (17 years old: cannot book 18+)
--    tharushi@gmail.com       Customer  (adult)
--    ishan@gmail.com          Customer  (adult)
--
--  Seat ids for reference (from schema.sql):
--    A1-A8 = 1-8,  B1-B8 = 9-16,  C1-C8 = 17-24,  D1-D8 = 25-32,  E1-E8 = 33-40
-- =====================================================================

USE movie_ticketing;


-- ---------------------------------------------------------------------
-- Staff and customers
-- ---------------------------------------------------------------------
INSERT INTO users (user_id, name, email, password_hash, role, is_active) VALUES
  (2, 'Saman Silva',          'saman@abccinema.lk',  'wJl8sy9XQ1lTDGkn83iCVQ==:Sd3hE82vJBEzsarI2Isu0xSK2CENfViWP+JVGniKZmc=', 'CASHIER',  TRUE),
  (3, 'Dilani Fernando',      'dilani@abccinema.lk', 'cklY70yr677St3dJztp2ww==:PeTFC8tC1ZU6XaWLUgk3U9biwekmx6tu2DScbyLsuHQ=', 'CASHIER',  TRUE),
  (4, 'Ruwan Jayasuriya',     'ruwan@abccinema.lk',  'iinMIDyyB+Gn0FSXpA27Tw==:eYMo2uX1XBqLWrytH5PN7yBVivFUZYUpnvWt/hMABsk=', 'CASHIER',  FALSE),
  (5, 'Amaya Perera',         'amaya@gmail.com',     'Emx6ZW5syw4G+trws+BoHw==:E/Mg2UNNUIhtN1JR+x6E2ipsvmpqVUS55xV4iECOi8A=', 'CUSTOMER', TRUE),
  (6, 'Kavindu Wijesinghe',   'kavindu@gmail.com',   'mhXF2xq3ZqeHnLs/xl+/GA==:CLCTRnduQHswvqLiLoSIE01ZvtKIqlqsVfMhVynvyX0=', 'CUSTOMER', TRUE),
  (7, 'Tharushi Gunawardena', 'tharushi@gmail.com',  'sROWHKVQ2sJC1e4nLP6XNQ==:a2yvq5ZKdrvEqcxA+SzhGG0sO3Q0eZEEH83kfoV4R9Y=', 'CUSTOMER', TRUE),
  (8, 'Ishan Rathnayake',     'ishan@gmail.com',     'BXo4sFsXD4Juz3MrZHO4Cg==:l7qj2uQvVjxo7R9lTuDwX3uUX3Soz4JxgarcHdK6KZ0=', 'CUSTOMER', TRUE);

INSERT INTO cashiers (cashier_id, user_id) VALUES
  (1, 2),   -- Saman
  (2, 3),   -- Dilani
  (3, 4);   -- Ruwan (deactivated, but his past sales stay in the reports)

INSERT INTO customers (customer_id, user_id, nic, phone, date_of_birth) VALUES
  (1, 5, '199856701234', '0771234567', '1998-03-02'),   -- Amaya
  (2, 6, '200907402345', '0712345678', '2009-03-14'),   -- Kavindu, 17
  (3, 7, '905671234V',   '0759876543', '1990-03-07'),   -- Tharushi (old NIC format)
  (4, 8, '200112303456', '0768765432', '2001-05-02');   -- Ishan


-- ---------------------------------------------------------------------
-- Movies (all created by manager 1)
-- ---------------------------------------------------------------------
INSERT INTO movies (movie_id, title, genre, description, duration_min, is_18_plus, trailer_url, base_price, status, created_by) VALUES
  (1, 'The Last Monsoon', 'Drama',
      'A tea-estate family waits for the rain that will save their harvest.',
      128, FALSE, 'https://example.com/trailers/the-last-monsoon', 1200.00, 'NOW_SHOWING', 1),
  (2, 'Galaxy Runners', 'Animation',
      'Three kids build a rocket from scrap and race across the solar system.',
      95,  FALSE, 'https://example.com/trailers/galaxy-runners',   1000.00, 'NOW_SHOWING', 1),
  (3, 'Night Shift', 'Thriller',
      'A hospital night nurse notices that patients keep disappearing.',
      112, TRUE,  'https://example.com/trailers/night-shift',      1500.00, 'NOW_SHOWING', 1),
  (4, 'Lanka Express', 'Action',
      'A train hijack on the Colombo to Badulla line.',
      140, FALSE, 'https://example.com/trailers/lanka-express',    1300.00, 'COMING_SOON', 1),
  (5, 'Old Harbour', 'Drama',
      'A fisherman''s last season before the harbour closes.',
      105, FALSE, NULL,                                             900.00, 'ARCHIVED',    1);
-- Old Harbour is ARCHIVED, not deleted: it still has a showtime and
-- tickets from 21 November (R06).


-- ---------------------------------------------------------------------
-- Showtimes. end_time = show_time + duration_min.
-- Every show starts at least 15 minutes after the previous one ends (R08).
-- 1-4 are in the past (they give the reports some history).
-- 5-10 are in the future (they can be booked).
-- ---------------------------------------------------------------------
INSERT INTO showtimes (showtime_id, movie_id, show_date, show_time, end_time, created_by) VALUES
  ( 1, 5, '2026-11-21', '10:00:00', '11:45:00', 1),   -- Old Harbour
  ( 2, 2, '2026-11-21', '13:00:00', '14:35:00', 1),   -- Galaxy Runners
  ( 3, 1, '2026-11-21', '18:00:00', '20:08:00', 1),   -- The Last Monsoon
  ( 4, 3, '2026-11-22', '20:00:00', '21:52:00', 1),   -- Night Shift (18+)
  ( 5, 2, '2026-12-05', '10:00:00', '11:35:00', 1),   -- Galaxy Runners
  ( 6, 1, '2026-12-05', '14:00:00', '16:08:00', 1),   -- The Last Monsoon
  ( 7, 3, '2026-12-05', '19:30:00', '21:22:00', 1),   -- Night Shift (18+)
  ( 8, 2, '2026-12-06', '11:00:00', '12:35:00', 1),   -- Galaxy Runners
  ( 9, 3, '2026-12-06', '21:00:00', '22:52:00', 1),   -- Night Shift (18+)
  (10, 1, '2027-01-02', '18:00:00', '20:08:00', 1);   -- The Last Monsoon


-- ---------------------------------------------------------------------
-- Bookings
--   customer_id set  = online booking
--   cashier_id set   = walk-in sale at the counter
--   hold_expires_at  = booking_time + 5 minutes
-- ---------------------------------------------------------------------
INSERT INTO bookings (booking_id, booking_reference, customer_id, cashier_id, showtime_id, status, booking_time, hold_expires_at, total_amount) VALUES
  -- Past shows: everything paid and printed
  ( 1, 'BK-4QX7MD', NULL, 1, 1, 'CONFIRMED', '2026-11-21 09:41:00', '2026-11-21 09:46:00', 1350.00),  -- walk-in, cash
  ( 2, 'BK-9TKR2A', 1, NULL, 2, 'CONFIRMED', '2026-11-19 20:15:00', '2026-11-19 20:20:00', 2000.00),  -- Amaya online
  ( 3, 'BK-H3ZP8W', NULL, 3, 3, 'CONFIRMED', '2026-11-21 17:30:00', '2026-11-21 17:35:00', 2400.00),  -- walk-in, counter card
  ( 4, 'BK-M6CV5N', 3, NULL, 4, 'CONFIRMED', '2026-11-20 21:05:00', '2026-11-20 21:10:00', 3000.00),  -- Tharushi online, 18+
  ( 5, 'BK-2LWJ9E', NULL, 1, 4, 'CONFIRMED', '2026-11-22 19:50:00', '2026-11-22 19:55:00', 1500.00),  -- walk-in, cash, 18+
  -- Future shows: online bookings waiting to be printed at the counter
  ( 6, 'BK-7F3K2Q', 1, NULL, 5, 'CONFIRMED', '2026-11-23 18:22:00', '2026-11-23 18:27:00', 1500.00),  -- Amaya
  ( 7, 'BK-R8NB4T', 4, NULL, 7, 'CONFIRMED', '2026-11-24 09:10:00', '2026-11-24 09:15:00', 3000.00),  -- Ishan, 18+
  ( 8, 'BK-X5GD3H', 2, NULL, 8, 'CONFIRMED', '2026-11-24 10:00:00', '2026-11-24 10:05:00', 1000.00),  -- Kavindu, all ages
  -- A hold that ran out but has not been cleaned up yet.
  -- Its seats B4 and B5 look taken until releaseExpiredHolds() runs.
  ( 9, 'BK-P2YQ6K', 3, NULL, 6, 'HELD',      '2026-11-24 08:00:00', '2026-11-24 08:05:00', 2400.00),  -- Tharushi
  -- A hold that was already cleaned up: status EXPIRED, no tickets, no payment.
  (10, 'BK-5JHC9V', 4, NULL, 6, 'EXPIRED',   '2026-11-23 15:00:00', '2026-11-23 15:05:00', 1200.00),  -- Ishan
  -- Advance walk-in sale for January, printed straight away (R25)
  (11, 'BK-W4TF7L', NULL, 2, 10, 'CONFIRMED', '2026-11-24 11:30:00', '2026-11-24 11:35:00', 1800.00); -- walk-in, cash


-- ---------------------------------------------------------------------
-- Tickets: one per seat. price = base_price (FULL) or 50% of it (HALF).
-- No HALF tickets on Night Shift, because it is 18+ (R11).
-- ---------------------------------------------------------------------
INSERT INTO tickets (booking_id, showtime_id, seat_id, ticket_type, price, printed_at, printed_by) VALUES
  -- Booking 1: Old Harbour, C4 + C5
  ( 1,  1, 20, 'FULL',  900.00, '2026-11-21 09:43:00', 1),
  ( 1,  1, 21, 'HALF',  450.00, '2026-11-21 09:43:00', 1),
  -- Booking 2: Galaxy Runners, D3 + D4 + D5, printed by Dilani on the day
  ( 2,  2, 27, 'FULL', 1000.00, '2026-11-21 12:40:00', 2),
  ( 2,  2, 28, 'HALF',  500.00, '2026-11-21 12:40:00', 2),
  ( 2,  2, 29, 'HALF',  500.00, '2026-11-21 12:40:00', 2),
  -- Booking 3: The Last Monsoon, E1 + E2, sold by Ruwan
  ( 3,  3, 33, 'FULL', 1200.00, '2026-11-21 17:32:00', 3),
  ( 3,  3, 34, 'FULL', 1200.00, '2026-11-21 17:32:00', 3),
  -- Booking 4: Night Shift, B6 + B7, printed by Saman
  ( 4,  4, 14, 'FULL', 1500.00, '2026-11-22 19:40:00', 1),
  ( 4,  4, 15, 'FULL', 1500.00, '2026-11-22 19:40:00', 1),
  -- Booking 5: Night Shift, A1
  ( 5,  4,  1, 'FULL', 1500.00, '2026-11-22 19:51:00', 1),
  -- Booking 6: Galaxy Runners 5 Dec, C3 + C4, not printed yet
  ( 6,  5, 19, 'FULL', 1000.00, NULL, NULL),
  ( 6,  5, 20, 'HALF',  500.00, NULL, NULL),
  -- Booking 7: Night Shift 5 Dec, D4 + D5, not printed yet
  ( 7,  7, 28, 'FULL', 1500.00, NULL, NULL),
  ( 7,  7, 29, 'FULL', 1500.00, NULL, NULL),
  -- Booking 8: Galaxy Runners 6 Dec, A4, not printed yet
  ( 8,  8,  4, 'FULL', 1000.00, NULL, NULL),
  -- Booking 9 (expired hold): The Last Monsoon 5 Dec, B4 + B5
  ( 9,  6, 12, 'FULL', 1200.00, NULL, NULL),
  ( 9,  6, 13, 'FULL', 1200.00, NULL, NULL),
  -- Booking 11: The Last Monsoon 2 Jan, E7 + E8
  (11, 10, 39, 'FULL', 1200.00, '2026-11-24 11:32:00', 2),
  (11, 10, 40, 'HALF',  600.00, '2026-11-24 11:32:00', 2);


-- ---------------------------------------------------------------------
-- Payments: one per CONFIRMED booking, amount = booking total.
-- Bookings 9 (HELD) and 10 (EXPIRED) were never paid, so they have none.
-- ---------------------------------------------------------------------
INSERT INTO payments (booking_id, payment_method, at_counter, amount, status, payment_time, card_last4, amount_received, change_given) VALUES
  ( 1, 'CASH', TRUE,  1350.00, 'SUCCESS', '2026-11-21 09:43:00', NULL,   1500.00, 150.00),
  ( 2, 'CARD', FALSE, 2000.00, 'SUCCESS', '2026-11-19 20:17:30', '4242', NULL,    NULL),
  ( 3, 'CARD', TRUE,  2400.00, 'SUCCESS', '2026-11-21 17:32:00', NULL,   NULL,    NULL),
  ( 4, 'CARD', FALSE, 3000.00, 'SUCCESS', '2026-11-20 21:06:40', '1881', NULL,    NULL),
  ( 5, 'CASH', TRUE,  1500.00, 'SUCCESS', '2026-11-22 19:51:00', NULL,   2000.00, 500.00),
  ( 6, 'CARD', FALSE, 1500.00, 'SUCCESS', '2026-11-23 18:24:00', '4242', NULL,    NULL),
  ( 7, 'CARD', FALSE, 3000.00, 'SUCCESS', '2026-11-24 09:12:00', '0005', NULL,    NULL),
  ( 8, 'CARD', FALSE, 1000.00, 'SUCCESS', '2026-11-24 10:02:00', '7777', NULL,    NULL),
  (11, 'CASH', TRUE,  1800.00, 'SUCCESS', '2026-11-24 11:32:00', NULL,   2000.00, 200.00);
