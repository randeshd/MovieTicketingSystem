package com.movieticketing;

import com.movieticketing.dao.*;
import com.movieticketing.model.*;
import com.movieticketing.service.BookingService;
import java.util.List;

public class MovieTicketingSystem {
    public static void main(String[] args) throws Exception {
        UserDAO users = new UserDAO();
        Customer amaya   = (Customer) users.login("amaya@gmail.com", "Password@123");
        Customer kavindu = (Customer) users.login("kavindu@gmail.com", "Password@123"); // 17 years old
        Cashier saman    = (Cashier)  users.login("saman@abccinema.lk", "Password@123");

        List<Seat> seats = new SeatDAO().findAll();          // index 0 = A1 ... 39 = E8
        Movie galaxy = null, night = null;
        for (Movie m : new MovieDAO().findNowShowing()) {
            if (m.getTitle().equals("Galaxy Runners")) galaxy = m;
            if (m.getTitle().equals("Night Shift")) night = m;
        }
        Showtime galaxyShow = new ShowtimeDAO().findUpcomingByMovie(galaxy.getMovieId()).get(0);
        Showtime nightShow  = new ShowtimeDAO().findUpcomingByMovie(night.getMovieId()).get(0);
        BookingService service = new BookingService();

        // 1. Online: Amaya holds E7 (Full) + E8 (Half), then pays by card
        try {
            Booking b = service.holdOnline(amaya, galaxyShow, List.of(seats.get(38), seats.get(39)),
                                           List.of(TicketType.FULL, TicketType.HALF));
            service.payOnline(b, "4242");
            System.out.println("1 OK   " + b.getReference() + "  total Rs. " + b.getTotalAmount() + "  " + b.getStatus());
        } catch (Exception e) { System.out.println("1 FAIL " + e.getMessage()); }

        // 2. The same seats again: must be refused
        try {
            service.holdOnline(amaya, galaxyShow, List.of(seats.get(39)), List.of(TicketType.FULL));
            System.out.println("2 WRONG: seat was sold twice");
        } catch (Exception e) { System.out.println("2 OK   refused: " + e.getMessage()); }

        // 3. Kavindu (17) tries the 18+ movie: must be refused
        try {
            service.holdOnline(kavindu, nightShow, List.of(seats.get(0)), List.of(TicketType.FULL));
            System.out.println("3 WRONG: under-18 booked an 18+ movie");
        } catch (Exception e) { System.out.println("3 OK   refused: " + e.getMessage()); }

        // 4. Counter: Saman sells E6 for cash, customer gives Rs. 5000
        try {
            Booking c = service.sellAtCounter(saman, galaxyShow, List.of(seats.get(37)),
                                              List.of(TicketType.FULL), new CashPayment(0, 5000));
            CashPayment cash = (CashPayment) c.getPayment();
            System.out.println("4 OK   " + c.getReference() + "  change Rs. " + cash.getChangeGiven());

            // 5. Cashier looks it up by reference and prints the ticket
            Booking found = new BookingDAO().findByReference(c.getReference());
            Ticket t = found.getTickets().get(0);
            new BookingDAO().markTicketPrinted(t.getTicketId(), saman.getCashierId());
            saman.printTicket(t);
        } catch (Exception e) { System.out.println("4/5 FAIL " + e.getMessage()); }
    }
}
