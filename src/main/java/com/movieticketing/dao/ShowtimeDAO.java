package com.movieticketing.dao;

import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Showtime;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeDAO {

    private static final String SELECT =
        "SELECT s.showtime_id, s.show_date, s.show_time, s.end_time, "
      + "m.movie_id, m.title, m.genre, m.description, m.duration_min, m.is_18_plus, "
      + "m.trailer_url, m.base_price, m.status "
      + "FROM showtimes s JOIN movies m ON m.movie_id = s.movie_id ";

    private static final LocalTime FIRST_SHOW = LocalTime.of(9, 0);
    private static final LocalTime LAST_SHOW = LocalTime.of(22, 30);

    public List<Showtime> findByDate(LocalDate date) throws SQLException {
        return query(SELECT + "WHERE s.show_date = ? ORDER BY s.show_time", Date.valueOf(date));
    }

    /** For the customer: future shows of one movie. */
    public List<Showtime> findUpcomingByMovie(int movieId) throws SQLException {
        List<Showtime> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT
                 + "WHERE m.movie_id = ? AND TIMESTAMP(s.show_date, s.show_time) > NOW() "
                 + "ORDER BY s.show_date, s.show_time")) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void add(Showtime showtime, int managerId) throws ValidationException, SQLException {
        validate(showtime);
        String sql = "INSERT INTO showtimes (movie_id, show_date, show_time, end_time, created_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, showtime);
            ps.setInt(5, managerId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) showtime.setShowtimeId(keys.getInt(1));
            }
        }
    }

    public void update(Showtime showtime) throws ValidationException, SQLException {
        validate(showtime);
        String sql = "UPDATE showtimes SET movie_id = ?, show_date = ?, show_time = ?, end_time = ? WHERE showtime_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            fill(ps, showtime);
            ps.setInt(5, showtime.getShowtimeId());
            ps.executeUpdate();
        }
    }

    // ---- helpers ----

    /** Opening hours (R07) and no clash with another show in the one hall (R08). */
    private void validate(Showtime s) throws ValidationException, SQLException {
        if (s.getMovie() == null || s.getShowDate() == null || s.getShowTime() == null) {
            throw new ValidationException("Choose a movie, date and time.");
        }
        if (s.getShowTime().isBefore(FIRST_SHOW) || s.getShowTime().isAfter(LAST_SHOW)) {
            throw new ValidationException("Shows must start between 09:00 and 22:30.");
        }
        s.setEndTime(s.getShowTime().plusMinutes(s.getMovie().getDurationMin()));
        // A late show can run past midnight, so check the day before, the day itself and the day after.
        for (int d = -1; d <= 1; d++) {
            for (Showtime other : findByDate(s.getShowDate().plusDays(d))) {
                if (other.getShowtimeId() != s.getShowtimeId() && s.overlaps(other)) {
                    throw new ValidationException("Clashes with " + other.getMovie().getTitle()
                            + " at " + other + " (shows need a 15-minute gap).");
                }
            }
        }
    }

    private void fill(PreparedStatement ps, Showtime s) throws SQLException {
        ps.setInt(1, s.getMovie().getMovieId());
        ps.setDate(2, Date.valueOf(s.getShowDate()));
        ps.setTime(3, Time.valueOf(s.getShowTime()));
        ps.setTime(4, Time.valueOf(s.getEndTime()));
    }

    private List<Showtime> query(String sql, Date date) throws SQLException {
        List<Showtime> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    static Showtime map(ResultSet rs) throws SQLException {
        return new Showtime(rs.getInt("showtime_id"), MovieDAO.map(rs),
                rs.getDate("show_date").toLocalDate(),
                rs.getTime("show_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime());
    }
}
