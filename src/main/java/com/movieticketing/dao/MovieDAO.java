package com.movieticketing.dao;

import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Movie;
import com.movieticketing.model.MovieStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    private static final String COLUMNS =
        "m.movie_id, m.title, m.genre, m.description, m.duration_min, m.is_18_plus, "
      + "m.trailer_url, m.base_price, m.status";

    public List<Movie> findAll() throws SQLException {
        return query("SELECT " + COLUMNS + " FROM movies m ORDER BY m.title");
    }

    /** What customers see: only NOW_SHOWING movies. */
    public List<Movie> findNowShowing() throws SQLException {
        return query("SELECT " + COLUMNS + " FROM movies m WHERE m.status = 'NOW_SHOWING' ORDER BY m.title");
    }

    public void add(Movie movie, int managerId) throws ValidationException, SQLException {
        validate(movie);
        String sql = "INSERT INTO movies (title, genre, description, duration_min, is_18_plus, "
                   + "trailer_url, base_price, status, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, movie);
            ps.setInt(9, managerId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) movie.setMovieId(keys.getInt(1));
            }
        }
    }

    /** Movies are never deleted: set status to ARCHIVED instead (R06). */
    public void update(Movie movie) throws ValidationException, SQLException {
        validate(movie);
        String sql = "UPDATE movies SET title = ?, genre = ?, description = ?, duration_min = ?, "
                   + "is_18_plus = ?, trailer_url = ?, base_price = ?, status = ? WHERE movie_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            fill(ps, movie);
            ps.setInt(9, movie.getMovieId());
            ps.executeUpdate();
        }
    }

    /** Turns the current row into a Movie. Package-private so ShowtimeDAO can reuse it. */
    static Movie map(ResultSet rs) throws SQLException {
        return new Movie(rs.getInt("movie_id"), rs.getString("title"), rs.getString("genre"),
                rs.getString("description"), rs.getInt("duration_min"), rs.getBoolean("is_18_plus"),
                rs.getString("trailer_url"), rs.getDouble("base_price"),
                MovieStatus.valueOf(rs.getString("status")));
    }

    private List<Movie> query(String sql) throws SQLException {
        List<Movie> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private void fill(PreparedStatement ps, Movie m) throws SQLException {
        ps.setString(1, m.getTitle().trim());
        ps.setString(2, m.getGenre().trim());
        ps.setString(3, m.getDescription());
        ps.setInt(4, m.getDurationMin());
        ps.setBoolean(5, m.is18Plus());
        ps.setString(6, m.getTrailerUrl());
        ps.setDouble(7, m.getBasePrice());
        ps.setString(8, m.getStatus().name());
    }

    private void validate(Movie m) throws ValidationException {
        if (m.getTitle() == null || m.getTitle().isBlank()) throw new ValidationException("Title is required.");
        if (m.getGenre() == null || m.getGenre().isBlank()) throw new ValidationException("Genre is required.");
        if (m.getDurationMin() < 1 || m.getDurationMin() > 600) throw new ValidationException("Duration must be 1 to 600 minutes.");
        if (m.getBasePrice() <= 0) throw new ValidationException("Price must be more than 0.");
        if (m.getStatus() == null) throw new ValidationException("Choose a status.");
    }
}
