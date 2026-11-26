package com.movieticketing.dao;

import com.movieticketing.exception.SeatUnavailableException;
import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Booking;
import com.movieticketing.model.BookingStatus;
import com.movieticketing.model.CardPayment;
import com.movieticketing.model.CashPayment;
import com.movieticketing.model.Payment;
import com.movieticketing.model.Seat;
import com.movieticketing.model.Showtime;
import com.movieticketing.model.Ticket;
import com.movieticketing.model.TicketType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

public class BookingDAO {

    /**
     * Saves the booking (status HELD, 5-minute hold) and one ticket row per seat, in ONE transaction.
     * If someone else already has a seat, MySQL rejects the ticket (error 1062) and nothing is saved.
     */
    public void holdSeats(Booking booking) throws SeatUnavailableException, SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO bookings (booking_reference, customer_id, cashier_id, showtime_id, status, "
                      + "booking_time, hold_expires_at, total_amount) VALUES (?, ?, ?, ?, 'HELD', ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, booking.getReference());
                    if (booking.getCustomer() != null) ps.setInt(2, booking.getCustomer().getCustomerId());
                    else ps.setNull(2, Types.INTEGER);
                    if (booking.getCashier() != null) ps.setInt(3, booking.getCashier().getCashierId());
                    else ps.setNull(3, Types.INTEGER);
                    ps.setInt(4, booking.getShowtime().getShowtimeId());
                    ps.setTimestamp(5, Timestamp.valueOf(booking.getBookingTime()));
                    ps.setTimestamp(6, Timestamp.valueOf(booking.getHoldExpiresAt()));
                    ps.setDouble(7, booking.getTotalAmount());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        booking.setBookingId(keys.getInt(1));
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO tickets (booking_id, showtime_id, seat_id, ticket_type, price) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    for (Ticket t : booking.getTickets()) {
                        ps.setInt(1, booking.getBookingId());
                        ps.setInt(2, booking.getShowtime().getShowtimeId());
                        ps.setInt(3, t.getSeat().getSeatId());
                        ps.setString(4, t.getType().name());
                        ps.setDouble(5, t.getPrice());
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            keys.next();
                            t.setTicketId(keys.getInt(1));
                        }
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                if (e.getErrorCode() == 1062 && e.getMessage().contains("uq_tickets_seat_per_show")) {
                    throw new SeatUnavailableException("Sorry, one of those seats was just taken. Please choose again.");
                }
                throw e; // includes a duplicate booking reference: the service retries with a new one
            }
        }
    }

    /**
     * Saves the payment and marks the booking CONFIRMED, in ONE transaction.
     * Refused if the 5-minute hold has already run out.
     */
    public void confirm(Booking booking) throws SeatUnavailableException, SQLException {
        Payment p = booking.getPayment();
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE bookings SET status = 'CONFIRMED' "
                      + "WHERE booking_id = ? AND status = 'HELD' AND hold_expires_at >= NOW()")) {
                    ps.setInt(1, booking.getBookingId());
                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        throw new SeatUnavailableException("Your 5-minute seat hold ran out. Please choose your seats again.");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO payments (booking_id, payment_method, at_counter, amount, status, payment_time, "
                      + "card_last4, amount_received, change_given) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, booking.getBookingId());
                    ps.setString(2, p.getMethod());
                    ps.setBoolean(3, p.isAtCounter());
                    ps.setDouble(4, p.getAmount());
                    ps.setString(5, p.getStatus().name());
                    ps.setTimestamp(6, Timestamp.valueOf(p.getPaymentTime()));
                    // POLYMORPHISM in reverse: check the real type to fill its own columns.
                    if (p instanceof CardPayment) {
                        CardPayment card = (CardPayment) p;
                        if (card.getCardLast4() == null || card.getCardLast4().isBlank()) ps.setNull(7, Types.CHAR);
                        else ps.setString(7, card.getCardLast4());
                        ps.setNull(8, Types.DECIMAL);
                        ps.setNull(9, Types.DECIMAL);
                    } else {
                        CashPayment cash = (CashPayment) p;
                        ps.setNull(7, Types.CHAR);
                        ps.setDouble(8, cash.getAmountReceived());
                        ps.setDouble(9, cash.getChangeGiven());
                    }
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) p.setPaymentId(keys.getInt(1));
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Frees the seats of every hold that ran out (query C4). Returns how many bookings expired. */
    public int releaseExpiredHolds() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement(
                     "DELETE t FROM tickets t JOIN bookings b ON b.booking_id = t.booking_id "
                   + "WHERE b.status = 'HELD' AND b.hold_expires_at < NOW()");
                 PreparedStatement upd = con.prepareStatement(
                     "UPDATE bookings SET status = 'EXPIRED' WHERE status = 'HELD' AND hold_expires_at < NOW()")) {
                del.executeUpdate();
                int expired = upd.executeUpdate();
                con.commit();
                return expired;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** What the cashier looks up to print tickets. Returns null if the reference doesn't exist. */
    public Booking findByReference(String ref) throws SQLException {
        String sql = "SELECT b.booking_id, b.booking_reference, b.status AS booking_status, b.booking_time, b.hold_expires_at, b.total_amount, "
                   + "s.showtime_id, s.show_date, s.show_time, s.end_time, "
                   + "m.movie_id, m.title, m.genre, m.description, m.duration_min, m.is_18_plus, m.trailer_url, m.base_price, m.status, "
                   + "t.ticket_id, t.ticket_type, t.price, t.printed_at, "
                   + "se.seat_id, se.row_letter, se.seat_number "
                   + "FROM bookings b "
                   + "JOIN showtimes s ON s.showtime_id = b.showtime_id "
                   + "JOIN movies m    ON m.movie_id    = s.movie_id "
                   + "LEFT JOIN tickets t ON t.booking_id = b.booking_id "
                   + "LEFT JOIN seats se  ON se.seat_id   = t.seat_id "
                   + "WHERE b.booking_reference = ? ORDER BY se.row_letter, se.seat_number";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ref.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                Booking booking = null;
                while (rs.next()) {
                    if (booking == null) {
                        Showtime show = ShowtimeDAO.map(rs);
                        booking = new Booking(rs.getString("booking_reference"), show);
                        booking.setBookingId(rs.getInt("booking_id"));
                        booking.setStatus(BookingStatus.valueOf(rs.getString("booking_status")));
                        booking.setBookingTime(rs.getTimestamp("booking_time").toLocalDateTime());
                        Timestamp hold = rs.getTimestamp("hold_expires_at");
                        booking.setHoldExpiresAt(hold == null ? null : hold.toLocalDateTime());
                    }
                    if (rs.getObject("ticket_id") != null) {
                        Seat seat = new Seat(rs.getInt("seat_id"), rs.getString("row_letter").charAt(0), rs.getInt("seat_number"));
                        Ticket t = new Ticket(seat, booking.getShowtime(),
                                TicketType.valueOf(rs.getString("ticket_type")), rs.getDouble("price"));
                        t.setTicketId(rs.getInt("ticket_id"));
                        Timestamp printed = rs.getTimestamp("printed_at");
                        if (printed != null) t.setPrintedAt(printed.toLocalDateTime());
                        booking.addTicket(t);
                    }
                }
                if (booking != null) booking.setTotalAmount(booking.calculateTotal());
                return booking;
            }
        }
    }

    /** A ticket can be printed once, and only for a paid booking (R24). */
    public void markTicketPrinted(int ticketId, int cashierId) throws ValidationException, SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE tickets t JOIN bookings b ON b.booking_id = t.booking_id "
               + "SET t.printed_at = NOW(), t.printed_by = ? "
               + "WHERE t.ticket_id = ? AND t.printed_at IS NULL AND b.status = 'CONFIRMED'")) {
            ps.setInt(1, cashierId);
            ps.setInt(2, ticketId);
            if (ps.executeUpdate() == 0) {
                throw new ValidationException("This ticket was already printed, or the booking is not paid.");
            }
        }
    }
}
