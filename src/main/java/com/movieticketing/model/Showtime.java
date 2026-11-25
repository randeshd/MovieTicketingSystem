package com.movieticketing.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Showtime {

    public static final int CLEANING_GAP_MIN = 15;
    public static final int ONLINE_CLOSES_MIN_BEFORE = 15;

    private int showtimeId;
    private Movie movie;
    private LocalDate showDate;
    private LocalTime showTime;
    private LocalTime endTime;

    public Showtime() { }

    public Showtime(int showtimeId, Movie movie, LocalDate showDate, LocalTime showTime, LocalTime endTime) {
        this.showtimeId = showtimeId;
        this.movie = movie;
        this.showDate = showDate;
        this.showTime = showTime;
        this.endTime = endTime;
    }

    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(showDate, showTime);
    }

    /** Uses the movie length, so a 22:30 show that ends after midnight still works. */
    public LocalDateTime getEndDateTime() {
        return getStartDateTime().plusMinutes(movie.getDurationMin());
    }

    /** True if the two shows (plus the 15-minute cleaning gap) clash in the one hall. */
    public boolean overlaps(Showtime other) {
        LocalDateTime myEnd = getEndDateTime().plusMinutes(CLEANING_GAP_MIN);
        LocalDateTime otherEnd = other.getEndDateTime().plusMinutes(CLEANING_GAP_MIN);
        return getStartDateTime().isBefore(otherEnd) && other.getStartDateTime().isBefore(myEnd);
    }

    public boolean isOnlineBookingOpen() {
        return LocalDateTime.now().isBefore(getStartDateTime().minusMinutes(ONLINE_CLOSES_MIN_BEFORE));
    }

    @Override
    public String toString() { return showDate + " " + showTime; }

    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }
    public LocalTime getShowTime() { return showTime; }
    public void setShowTime(LocalTime showTime) { this.showTime = showTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}