-- =====================================================================
--  ABC Cinema - test-queries.sql
--  Run these ONE AT A TIME in MySQL Workbench (click inside a statement,
--  then Ctrl+Enter / Cmd+Enter runs just that one).
--  Part A reads data. Part B shows the rules the database enforces:
--  every statement in Part B is SUPPOSED to fail with an error.
--  Part C is what the Java DAOs will run (the ? marks are filled in by
--  PreparedStatement; here they are replaced with sample values).
--  Run schema.sql + sample-data.sql again afterwards to reset.
-- =====================================================================

USE movie_ticketing;

-- ---------------------------------------------------------------------
-- PART A: look at the data
-- ---------------------------------------------------------------------

-- A1. Every login and its role. Expect 8 rows; Ruwan has is_active = 0.
SELECT user_id, name, email, role, is_active FROM users;

-- A2. What a customer sees: Now showing movies and their future showtimes.
SELECT m.title, m.is_18_plus, s.show_date, s.show_time, m.base_price
FROM showtimes s
JOIN movies m ON m.movie_id = s.movie_id
WHERE m.status = 'NOW_SHOWING'
  AND TIMESTAMP(s.show_date, s.show_time) > NOW()
ORDER BY s.show_date, s.show_time;

-- A3. Seat map for showtime 6 (The Last Monsoon, 5 Dec).
--     Taken = any ticket row exists (HELD or CONFIRMED). Expect B4, B5 taken.
SELECT CONCAT(se.row_letter, se.seat_number) AS seat,
       IF(t.ticket_id IS NULL, 'free', 'TAKEN') AS state
FROM seats se
LEFT JOIN tickets t ON t.seat_id = se.seat_id AND t.showtime_id = 6
ORDER BY se.row_letter, se.seat_number;


-- ---------------------------------------------------------------------
-- PART B: rules the database enforces. EACH ONE MUST FAIL.
-- ---------------------------------------------------------------------

-- B1. Double booking (R15, R28): seat D4 (seat_id 28) is already sold for
--     showtime 7. Expect: Error 1062 Duplicate entry '7-28' for key 'uq_tickets_seat_per_show'
INSERT INTO tickets (booking_id, showtime_id, seat_id, ticket_type, price)
VALUES (8, 7, 28, 'FULL', 1500.00);

-- B2. Same email twice (R29). Expect: Error 1062 Duplicate entry ... 'uq_users_email'
INSERT INTO users (name, email, password_hash, role)
VALUES ('Copy Cat', 'amaya@gmail.com', 'x:y', 'CUSTOMER');

-- B3. A show starting at 11 PM (R07). Expect: Error 3819 Check constraint 'chk_showtimes_hours' is violated
INSERT INTO showtimes (movie_id, show_date, show_time, end_time, created_by)
VALUES (1, '2026-12-10', '23:00:00', '01:08:00', 1);

-- B4. Deleting a movie that has showtimes (R06). Expect: Error 1451 Cannot delete ... a foreign key constraint fails
DELETE FROM movies WHERE movie_id = 5;

-- B5. Cash that does not cover the total (R21). Expect: Error 3819 'chk_payments_method'
--     (booking 9 has no payment yet, so only the CHECK can stop this)
INSERT INTO payments (booking_id, payment_method, at_counter, amount, amount_received, change_given)
VALUES (9, 'CASH', TRUE, 2400.00, 2000.00, 0.00);

-- B6. A seat that does not exist (row F). Expect: Error 3819 'chk_seats_row'
INSERT INTO seats (row_letter, seat_number) VALUES ('F', 1);

-- B7. A booking with neither a customer nor a cashier. Expect: Error 3819 'chk_bookings_who'
INSERT INTO bookings (booking_reference, showtime_id, status, hold_expires_at, total_amount)
VALUES ('BK-TEST01', 5, 'HELD', NOW(), 1000.00);


-- ---------------------------------------------------------------------
-- PART C: the queries the Java DAOs will use
-- ---------------------------------------------------------------------

-- C1. UserDAO.login: find the account by email. Java then checks the
--     password against password_hash and refuses inactive accounts.
SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.is_active
FROM users u
WHERE u.email = 'amaya@gmail.com';

-- C2. SeatDAO.findTakenSeatIds(6): ids of taken seats. Expect 12, 13.
SELECT seat_id FROM tickets WHERE showtime_id = 6;

-- C3. BookingDAO.findByReference: what the cashier sees for BK-7F3K2Q.
SELECT b.booking_reference, b.status, m.title, s.show_date, s.show_time,
       CONCAT(se.row_letter, se.seat_number) AS seat, t.ticket_type, t.price,
       t.printed_at, m.is_18_plus OR t.ticket_type = 'HALF' AS id_check_required
FROM bookings b
JOIN showtimes s ON s.showtime_id = b.showtime_id
JOIN movies m    ON m.movie_id    = s.movie_id
JOIN tickets t   ON t.booking_id  = b.booking_id
JOIN seats se    ON se.seat_id    = t.seat_id
WHERE b.booking_reference = 'BK-7F3K2Q';

-- C4. BookingDAO.releaseExpiredHolds(): two statements, run in one
--     transaction in Java. Booking 9's hold ended, so B4 and B5 become free.
--     After this, run C2 again: it returns no rows.
DELETE t FROM tickets t
JOIN bookings b ON b.booking_id = t.booking_id
WHERE b.status = 'HELD' AND b.hold_expires_at < NOW();

UPDATE bookings SET status = 'EXPIRED'
WHERE status = 'HELD' AND hold_expires_at < NOW();

-- C5. ReportDAO.dailySales('2026-11-21') (R32). Money counts on the day it
--     was PAID, not the show day. Expect: online card 0 (Amaya paid for her
--     21 Nov show on 19 Nov), counter cash 1350, counter card 2400,
--     3 Full and 1 Half tickets. Try '2026-11-24' too: online card 4000, cash 1800.
SELECT
  SUM(CASE WHEN p.payment_method = 'CARD' AND NOT p.at_counter THEN p.amount ELSE 0 END) AS online_card_total,
  SUM(CASE WHEN p.payment_method = 'CASH'                      THEN p.amount ELSE 0 END) AS counter_cash_total,
  SUM(CASE WHEN p.payment_method = 'CARD' AND p.at_counter     THEN p.amount ELSE 0 END) AS counter_card_total
FROM payments p
WHERE p.status = 'SUCCESS' AND DATE(p.payment_time) = '2026-11-21';

SELECT
  SUM(t.ticket_type = 'FULL') AS full_tickets,
  SUM(t.ticket_type = 'HALF') AS half_tickets
FROM tickets t
JOIN payments p ON p.booking_id = t.booking_id
WHERE p.status = 'SUCCESS' AND DATE(p.payment_time) = '2026-11-21';

-- C6. ReportDAO.salesPerMovie(from, to) (R33), by show date.
SELECT m.title, COUNT(t.ticket_id) AS tickets, SUM(t.price) AS revenue
FROM tickets t
JOIN bookings b  ON b.booking_id  = t.booking_id
JOIN showtimes s ON s.showtime_id = t.showtime_id
JOIN movies m    ON m.movie_id    = s.movie_id
WHERE b.status = 'CONFIRMED'
  AND s.show_date BETWEEN '2026-11-01' AND '2027-01-31'
GROUP BY m.movie_id, m.title
ORDER BY revenue DESC;

-- C7. ReportDAO.occupancy (R34): seats sold out of 40 for every showtime.
SELECT s.showtime_id, m.title, s.show_date, s.show_time,
       COUNT(t.ticket_id) AS sold, 40 - COUNT(t.ticket_id) AS free
FROM showtimes s
JOIN movies m ON m.movie_id = s.movie_id
LEFT JOIN bookings b ON b.showtime_id = s.showtime_id AND b.status = 'CONFIRMED'
LEFT JOIN tickets t  ON t.booking_id  = b.booking_id
GROUP BY s.showtime_id, m.title, s.show_date, s.show_time
ORDER BY s.show_date, s.show_time;

-- C8. SettingDAO.get / update: the Half ticket percentage (R10).
SELECT setting_value FROM settings WHERE setting_key = 'HALF_TICKET_PERCENT';
-- UPDATE settings SET setting_value = '60', updated_by = 1 WHERE setting_key = 'HALF_TICKET_PERCENT';
