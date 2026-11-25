package com.movieticketing;

import com.movieticketing.model.Movie;
import com.movieticketing.model.MovieStatus;
import com.movieticketing.model.TicketType;

public class MovieTicketingSystem {
    public static void main(String[] args) {
        Movie m = new Movie(1, "Night Shift", "Thriller", "A test movie", 135,
                            true, null, 1200.00, MovieStatus.NOW_SHOWING);
        System.out.println(m + " runs " + m.getFormattedDuration());
        System.out.println("Bookable? " + m.isBookable());
        System.out.println("Full: Rs. " + m.priceFor(TicketType.FULL, 50));
        System.out.println("Half: Rs. " + m.priceFor(TicketType.HALF, 50));
    }
}