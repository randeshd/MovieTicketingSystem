package com.movieticketing.dao;

import com.movieticketing.model.SalesReport;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Read-only queries for the manager's reports (R32 to R34). */
public class ReportDAO {

    /** Money counts on the day it was PAID, not the show day (R32). */
    public SalesReport dailySales(LocalDate date) throws SQLException {
        String money = "SELECT "
            + "COALESCE(SUM(CASE WHEN payment_method = 'CARD' AND NOT at_counter THEN amount END), 0) AS online_card, "
            + "COALESCE(SUM(CASE WHEN payment_method = 'CASH' THEN amount END), 0) AS counter_cash, "
            + "COALESCE(SUM(CASE WHEN payment_method = 'CARD' AND at_counter THEN amount END), 0) AS counter_card "
            + "FROM payments WHERE status = 'SUCCESS' AND DATE(payment_time) = ?";
        String tickets = "SELECT "
            + "COALESCE(SUM(t.ticket_type = 'FULL'), 0) AS full_tickets, "
            + "COALESCE(SUM(t.ticket_type = 'HALF'), 0) AS half_tickets "
            + "FROM tickets t JOIN payments p ON p.booking_id = t.booking_id "
            + "WHERE p.status = 'SUCCESS' AND DATE(p.payment_time) = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps1 = con.prepareStatement(money);
             PreparedStatement ps2 = con.prepareStatement(tickets)) {
            ps1.setDate(1, Date.valueOf(date));
            ps2.setDate(1, Date.valueOf(date));
            try (ResultSet m = ps1.executeQuery(); ResultSet t = ps2.executeQuery()) {
                m.next();
                t.next();
                return new SalesReport(date, date, m.getDouble("online_card"), m.getDouble("counter_cash"),
                        m.getDouble("counter_card"), t.getInt("full_tickets"), t.getInt("half_tickets"));
            }
        }
    }

    /** One row per movie: {title, tickets sold, revenue}, by show date (R33). Ready for a JTable. */
    public List<String[]> salesPerMovie(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT m.title, COUNT(t.ticket_id) AS tickets, SUM(t.price) AS revenue "
                   + "FROM tickets t "
                   + "JOIN bookings b  ON b.booking_id  = t.booking_id "
                   + "JOIN showtimes s ON s.showtime_id = t.showtime_id "
                   + "JOIN movies m    ON m.movie_id    = s.movie_id "
                   + "WHERE b.status = 'CONFIRMED' AND s.show_date BETWEEN ? AND ? "
                   + "GROUP BY m.movie_id, m.title ORDER BY revenue DESC";
        List<String[]> rows = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[] {
                        rs.getString("title"),
                        String.valueOf(rs.getInt("tickets")),
                        String.format("%.2f", rs.getDouble("revenue"))
                    });
                }
            }
        }
        return rows;
    }

    /** Seats sold (paid) for one show, out of 40 (R34). */
    public int occupancy(int showtimeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM tickets t JOIN bookings b ON b.booking_id = t.booking_id "
                   + "WHERE t.showtime_id = ? AND b.status = 'CONFIRMED'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
