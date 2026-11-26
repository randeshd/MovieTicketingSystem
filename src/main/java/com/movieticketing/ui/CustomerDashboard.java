package com.movieticketing.ui;

import com.movieticketing.dao.MovieDAO;
import com.movieticketing.dao.SeatDAO;
import com.movieticketing.dao.ShowtimeDAO;
import com.movieticketing.exception.PaymentException;
import com.movieticketing.exception.SeatUnavailableException;
import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Booking;
import com.movieticketing.model.Customer;
import com.movieticketing.model.Movie;
import com.movieticketing.model.Seat;
import com.movieticketing.model.Showtime;
import com.movieticketing.model.TicketType;
import com.movieticketing.service.BookingService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/** The customer picks a movie, a show and seats, then pays by card. */
public class CustomerDashboard extends JFrame {

    private final Customer customer;
    private final BookingService bookingService = new BookingService();

    private final DefaultListModel<Movie> movieModel = new DefaultListModel<>();
    private final JList<Movie> lstMovies = new JList<>(movieModel);
    private final JLabel lblDetails = new JLabel(" ");
    private final JComboBox<Showtime> cmbShowtimes = new JComboBox<>();
    private final SeatMapPanel seatMap = new SeatMapPanel();
    private final JLabel lblSelected = new JLabel("No seats chosen");
    private final JButton btnContinue = new JButton("Continue to payment");

    public CustomerDashboard(Customer customer) {
        super("ABC Cinema - " + customer.getName());
        this.customer = customer;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // TOP: welcome + logout
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        top.add(new JLabel("Welcome, " + customer.getName()), BorderLayout.WEST);
        JButton btnLogout = new JButton("Logout");
        top.add(btnLogout, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // LEFT: movies, details, showtimes
        lstMovies.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.setBorder(BorderFactory.createTitledBorder("Now showing"));
        left.add(new JScrollPane(lstMovies), BorderLayout.CENTER);
        JPanel leftBottom = new JPanel(new GridLayout(0, 1, 4, 4));
        leftBottom.add(lblDetails);
        leftBottom.add(new JLabel("Show:"));
        leftBottom.add(cmbShowtimes);
        left.add(leftBottom, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        // CENTRE: the seat map
        add(seatMap, BorderLayout.CENTER);

        // BOTTOM: selection + continue
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(lblSelected);
        bottom.add(btnContinue);
        add(bottom, BorderLayout.SOUTH);

        // EVENTS
        lstMovies.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) movieChosen(); });
        cmbShowtimes.addActionListener(e -> loadSeats());
        seatMap.setOnChange(this::updateSelection);
        btnContinue.addActionListener(e -> book());
        btnLogout.addActionListener(e -> { new LoginFrame().setVisible(true); dispose(); });

        loadMovies();
        setSize(950, 560);
        setLocationRelativeTo(null);
    }

    private void loadMovies() {
        try {
            movieModel.clear();
            for (Movie m : new MovieDAO().findNowShowing()) movieModel.addElement(m);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void movieChosen() {
        Movie m = lstMovies.getSelectedValue();
        if (m == null) return;
        lblDetails.setText("<html>" + m.getGenre() + " | " + m.getFormattedDuration()
                + (m.is18Plus() ? " | <b>18+</b>" : "") + "<br>Full ticket: Rs. "
                + String.format("%.2f", m.getBasePrice()) + "</html>");
        try {
            List<Showtime> shows = new ShowtimeDAO().findUpcomingByMovie(m.getMovieId());
            cmbShowtimes.setModel(new DefaultComboBoxModel<>(shows.toArray(new Showtime[0])));
            if (shows.isEmpty()) seatMap.clear(); else loadSeats();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadSeats() {
        Showtime show = (Showtime) cmbShowtimes.getSelectedItem();
        if (show == null) return;
        try {
            SeatDAO seatDAO = new SeatDAO();
            seatMap.load(seatDAO.findAll(), seatDAO.findTakenSeatIds(show.getShowtimeId()));
            updateSelection();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateSelection() {
        List<Seat> chosen = seatMap.getSelectedSeats();
        lblSelected.setText(chosen.isEmpty() ? "No seats chosen" : "Seats: " + chosen);
    }

    private void book() {
        Showtime show = (Showtime) cmbShowtimes.getSelectedItem();
        List<Seat> seats = seatMap.getSelectedSeats();
        if (show == null || seats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Choose a movie, a show and at least one seat.");
            return;
        }
        List<TicketType> types = TicketTypeDialog.ask(this, seats, show.getMovie().is18Plus());
        if (types == null) return;

        try {
            // Step 1: hold the seats for 5 minutes
            Booking booking = bookingService.holdOnline(customer, show, seats, types);
            String until = booking.getHoldExpiresAt().format(DateTimeFormatter.ofPattern("HH:mm"));

            // Step 2: pay
            String last4 = JOptionPane.showInputDialog(this,
                    String.format("Total: Rs. %.2f%nYour seats are held until %s.%n%nCard number - last 4 digits:",
                            booking.getTotalAmount(), until),
                    "Pay by card", JOptionPane.PLAIN_MESSAGE);
            if (last4 == null) {
                JOptionPane.showMessageDialog(this, "Payment cancelled. Your seats will be released at " + until + ".");
                loadSeats();
                return;
            }
            bookingService.payOnline(booking, last4.trim());
            JOptionPane.showMessageDialog(this,
                    "Booked! Your reference is " + booking.getReference()
                    + "\nShow it at the counter to collect your tickets.",
                    "Booking confirmed", JOptionPane.INFORMATION_MESSAGE);
        } catch (ValidationException | SeatUnavailableException | PaymentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot book", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            showError(ex);
        }
        loadSeats();
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
