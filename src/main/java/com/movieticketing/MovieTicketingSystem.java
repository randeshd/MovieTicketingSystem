package com.movieticketing;

import com.movieticketing.dao.*;
import com.movieticketing.model.*;

public class MovieTicketingSystem {
    public static void main(String[] args) throws Exception {
        for (Movie m : new MovieDAO().findNowShowing()) {
            System.out.println(m.getTitle() + " (" + m.getFormattedDuration() + ")");
            for (Showtime s : new ShowtimeDAO().findUpcomingByMovie(m.getMovieId())) {
                System.out.println("   " + s);
            }
        }
        System.out.println("Seats in hall: " + new SeatDAO().findAll().size());
        System.out.println("Taken for show 6: " + new SeatDAO().findTakenSeatIds(6));
        System.out.println("Half ticket %: " + new SettingDAO().getHalfTicketPercent());
    }
}