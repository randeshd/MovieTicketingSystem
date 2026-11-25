package com.movieticketing.model;

public class Movie {

    private int movieId;
    private String title;
    private String genre;
    private String description;
    private int durationMin;
    private boolean is18Plus;
    private String trailerUrl;
    private double basePrice;
    private MovieStatus status;

    public Movie() { }

    public Movie(int movieId, String title, String genre, String description, int durationMin,
                 boolean is18Plus, String trailerUrl, double basePrice, MovieStatus status) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.description = description;
        this.durationMin = durationMin;
        this.is18Plus = is18Plus;
        this.trailerUrl = trailerUrl;
        this.basePrice = basePrice;
        this.status = status;
    }

    /** 135 -> "2h 15m" */
    public String getFormattedDuration() {
        return (durationMin / 60) + "h " + (durationMin % 60) + "m";
    }

    public boolean isBookable() {
        return status == MovieStatus.NOW_SHOWING;
    }

    /** FULL = base price. HALF = base price x halfPercent / 100 (the manager's setting). */
    public double priceFor(TicketType type, double halfPercent) {
        if (type == TicketType.HALF) {
            return Math.round(basePrice * halfPercent / 100.0 * 100.0) / 100.0;
        }
        return basePrice;
    }

    @Override
    public String toString() { return title; } // so a drop-down list shows the title

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }
    public boolean is18Plus() { return is18Plus; }
    public void set18Plus(boolean is18Plus) { this.is18Plus = is18Plus; }
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }
    public MovieStatus getStatus() { return status; }
    public void setStatus(MovieStatus status) { this.status = status; }
}