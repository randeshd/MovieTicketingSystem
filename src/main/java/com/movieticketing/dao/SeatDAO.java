package com.movieticketing.dao;

import com.movieticketing.model.Seat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SeatDAO {

    /** All 40 seats, A1 to E8. */
    public List<Seat> findAll() throws SQLException {
        List<Seat> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT seat_id, row_letter, seat_number FROM seats ORDER BY row_letter, seat_number");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Seat(rs.getInt("seat_id"), rs.getString("row_letter").charAt(0), rs.getInt("seat_number")));
            }
        }
        return list;
    }

    /** Seats with a ticket row for this show (held OR paid) are taken. */
    public List<Integer> findTakenSeatIds(int showtimeId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT seat_id FROM tickets WHERE showtime_id = ?")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("seat_id"));
            }
        }
        return ids;
    }
}
