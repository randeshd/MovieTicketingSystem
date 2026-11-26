package com.movieticketing.service;

import com.movieticketing.dao.BookingDAO;
import com.movieticketing.dao.SettingDAO;
import com.movieticketing.exception.PaymentException;
import com.movieticketing.exception.SeatUnavailableException;
import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Booking;
import com.movieticketing.model.CardPayment;
import com.movieticketing.model.Cashier;
import com.movieticketing.model.Customer;
import com.movieticketing.model.Movie;
import com.movieticketing.model.Payment;
import com.movieticketing.model.Seat;
import com.movieticketing.model.Showtime;
import com.movieticketing.model.Ticket;
import com.movieticketing.model.TicketType;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;

/**
 * Runs the booking steps in the right order, so the screens stay simple.
 * Screens call this; this calls the DAOs. (ui -> service -> dao)
 */
public class BookingService {

    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O or 1/I mix-ups
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingDAO bookingDAO = new BookingDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    /** Step 1 online: check the rules and hold the seats for 5 minutes. */
    public Booking holdOnline(Customer customer, Showtime showtime, List<Seat> seats, List<TicketType> types)
            throws ValidationException, SeatUnavailableException, SQLException {
        if (!showtime.isOnlineBookingOpen()) {
            throw new ValidationException("Online booking closes 15 minutes before the show. Please buy at the counter.");
        }
        checkAge(customer, showtime.getMovie());
        Booking booking = build(showtime, seats, types);
        booking.setCustomer(customer);
        return save(booking);
    }

    /** Step 2 online: take the card payment and confirm (must be within the 5-minute hold). */
    public void payOnline(Booking booking, String cardLast4)
            throws PaymentException, SeatUnavailableException, SQLException {
        Payment payment = new CardPayment(booking.getTotalAmount(), cardLast4, false);
        payment.process();            // throws PaymentException if the card details are wrong
        booking.confirm(payment);
        bookingDAO.confirm(booking);  // throws SeatUnavailableException if the hold ran out
    }

    /** The cashier sells and is paid in one go. The payment is a CashPayment or a CardPayment. */
    public Booking sellAtCounter(Cashier cashier, Showtime showtime, List<Seat> seats, List<TicketType> types,
                                 Payment payment)
            throws ValidationException, SeatUnavailableException, PaymentException, SQLException {
        Booking booking = build(showtime, seats, types);
        booking.setCashier(cashier);
        payment.setAmount(booking.getTotalAmount());
        payment.process();            // POLYMORPHISM: runs CashPayment's or CardPayment's version
        booking = save(booking);
        booking.confirm(payment);
        bookingDAO.confirm(booking);
        return booking;
    }

    // ---- private steps ----

    private Booking build(Showtime showtime, List<Seat> seats, List<TicketType> types)
            throws ValidationException, SQLException {
        Movie movie = showtime.getMovie();
        if (!movie.isBookable()) {
            throw new ValidationException(movie.getTitle() + " is not showing at the moment.");
        }
        if (seats == null || seats.isEmpty() || seats.size() > Booking.MAX_SEATS) {
            throw new ValidationException("Choose between 1 and " + Booking.MAX_SEATS + " seats.");
        }
        if (types == null || types.size() != seats.size()) {
            throw new ValidationException("Choose Full or Half for every seat.");
        }
        double halfPercent = settingDAO.getHalfTicketPercent();
        Booking booking = new Booking(generateReference(), showtime);
        for (int i = 0; i < seats.size(); i++) {
            TicketType type = types.get(i);
            if (type == TicketType.HALF && movie.is18Plus()) {
                throw new ValidationException("Half tickets are not sold for 18+ movies.");
            }
            booking.addTicket(new Ticket(seats.get(i), showtime, type, movie.priceFor(type, halfPercent)));
        }
        return booking;
    }

    /** Frees expired holds first, then saves. Retries if the random reference already exists. */
    private Booking save(Booking booking) throws SeatUnavailableException, SQLException {
        bookingDAO.releaseExpiredHolds();
        for (int attempt = 1; ; attempt++) {
            try {
                bookingDAO.holdSeats(booking);
                return booking;
            } catch (SQLException e) {
                if (e.getErrorCode() != 1062 || attempt == 3) throw e;
                booking = rebuildWithNewReference(booking);
            }
        }
    }

    private Booking rebuildWithNewReference(Booking old) {
        Booking b = new Booking(generateReference(), old.getShowtime());
        b.setCustomer(old.getCustomer());
        b.setCashier(old.getCashier());
        for (Ticket t : old.getTickets()) b.addTicket(t);
        return b;
    }

    private void checkAge(Customer customer, Movie movie) throws ValidationException {
        if (!customer.canBook(movie)) {
            throw new ValidationException(movie.getTitle() + " is 18+. You must be 18 or older to book it.");
        }
    }

    /** e.g. BK-7F3K2Q */
    private String generateReference() {
        StringBuilder sb = new StringBuilder("BK-");
        for (int i = 0; i < 6; i++) {
            sb.append(REF_CHARS.charAt(RANDOM.nextInt(REF_CHARS.length())));
        }
        return sb.toString();
    }
}
