package com.movieticketing.ui;

import com.movieticketing.dao.BookingDAO;
import com.movieticketing.dao.MovieDAO;
import com.movieticketing.dao.SeatDAO;
import com.movieticketing.dao.ShowtimeDAO;
import com.movieticketing.exception.PaymentException;
import com.movieticketing.exception.SeatUnavailableException;
import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Booking;
import com.movieticketing.model.BookingStatus;
import com.movieticketing.model.CardPayment;
import com.movieticketing.model.Cashier;
import com.movieticketing.model.CashPayment;
import com.movieticketing.model.Movie;
import com.movieticketing.model.Payment;
import com.movieticketing.model.Seat;
import com.movieticketing.model.Showtime;
import com.movieticketing.model.Ticket;
import com.movieticketing.model.TicketType;
import com.movieticketing.service.BookingService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Two jobs, two tabs: sell at the counter, and issue tickets for online bookings. */
public class CashierDashboard extends JFrame {

    private final Cashier cashier;
    private final BookingService bookingService = new BookingService();
    private final BookingDAO bookingDAO = new BookingDAO();

    // "Sell tickets" tab
    private final JComboBox<Showtime> cmbShows = new JComboBox<>();
    private final SeatMapPanel seatMap = new SeatMapPanel();
    private final JRadioButton rdoCash = new JRadioButton("Cash", true);
    private final JRadioButton rdoCard = new JRadioButton("Card");
    private final JTextField txtReceived = new JTextField(8);

    // "Issue tickets" tab
    private final JTextField txtReference = new JTextField(12);
    private final JTextArea txtBooking = new JTextArea(14, 50);
    private Booking foundBooking;

    public CashierDashboard(Cashier cashier) {
        super("ABC Cinema - Cashier: " + cashier.getName());
        this.cashier = cashier;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        top.add(new JLabel("Cashier: " + cashier.getName()), BorderLayout.WEST);
        JButton btnLogout = new JButton("Logout");
        top.add(btnLogout, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sell tickets", buildSellTab());
        tabs.addTab("Issue tickets", buildIssueTab());
        add(tabs, BorderLayout.CENTER);

        btnLogout.addActionListener(e -> { new LoginFrame().setVisible(true); dispose(); });

        loadShows();
        setSize(950, 600);
        setLocationRelativeTo(null);
    }

    // ===================== SELL TAB =====================

    private JPanel buildSellTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Show:"));
        north.add(cmbShows);
        JButton btnRefresh = new JButton("Refresh");
        north.add(btnRefresh);
        panel.add(north, BorderLayout.NORTH);

        panel.add(seatMap, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ButtonGroup group = new ButtonGroup(); // only one of Cash/Card can be picked
        group.add(rdoCash);
        group.add(rdoCard);
        south.add(new JLabel("Payment:"));
        south.add(rdoCash);
        south.add(rdoCard);
        south.add(new JLabel("Cash received (Rs.):"));
        south.add(txtReceived);
        JButton btnSell = new JButton("Sell");
        south.add(btnSell);
        panel.add(south, BorderLayout.SOUTH);

        cmbShows.addActionListener(e -> loadSeats());
        btnRefresh.addActionListener(e -> loadSeats());
        rdoCash.addActionListener(e -> txtReceived.setEnabled(true));
        rdoCard.addActionListener(e -> txtReceived.setEnabled(false));
        btnSell.addActionListener(e -> sell());
        return panel;
    }

    private void loadShows() {
        try {
            List<Showtime> shows = new ArrayList<>();
            for (Movie m : new MovieDAO().findNowShowing()) {
                shows.addAll(new ShowtimeDAO().findUpcomingByMovie(m.getMovieId()));
            }
            shows.sort((a, b) -> a.getStartDateTime().compareTo(b.getStartDateTime()));
            cmbShows.setModel(new DefaultComboBoxModel<>(shows.toArray(new Showtime[0])));
            // show the movie title in the drop-down too
            cmbShows.setRenderer(new javax.swing.DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                        int index, boolean selected, boolean focus) {
                    String text = value == null ? "" : ((Showtime) value).getMovie().getTitle() + "  -  " + value;
                    return super.getListCellRendererComponent(list, text, index, selected, focus);
                }
            });
            loadSeats();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadSeats() {
        Showtime show = (Showtime) cmbShows.getSelectedItem();
        if (show == null) { seatMap.clear(); return; }
        try {
            SeatDAO seatDAO = new SeatDAO();
            seatMap.load(seatDAO.findAll(), seatDAO.findTakenSeatIds(show.getShowtimeId()));
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void sell() {
        Showtime show = (Showtime) cmbShows.getSelectedItem();
        List<Seat> seats = seatMap.getSelectedSeats();
        if (show == null || seats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Choose a show and at least one seat.");
            return;
        }
        List<TicketType> types = TicketTypeDialog.ask(this, seats, show.getMovie().is18Plus());
        if (types == null) return;

        // POLYMORPHISM: the service only sees "a Payment". We decide which kind here.
        Payment payment;
        if (rdoCash.isSelected()) {
            double received;
            try {
                received = Double.parseDouble(txtReceived.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter the cash received, for example 2000.");
                return;
            }
            payment = new CashPayment(0, received);
        } else {
            payment = new CardPayment(0, null, true); // card at the counter
        }

        try {
            Booking booking = bookingService.sellAtCounter(cashier, show, seats, types, payment);
            String msg = String.format("Sold! Reference %s%nTotal: Rs. %.2f", booking.getReference(), booking.getTotalAmount());
            if (payment instanceof CashPayment) {
                msg += String.format("%nChange: Rs. %.2f", ((CashPayment) payment).getChangeGiven());
            }
            JOptionPane.showMessageDialog(this, msg, "Sale complete", JOptionPane.INFORMATION_MESSAGE);
            printAll(bookingDAO.findByReference(booking.getReference()));
            txtReceived.setText("");
        } catch (ValidationException | SeatUnavailableException | PaymentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot sell", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            showError(ex);
        }
        loadSeats();
    }

    // ===================== ISSUE TAB =====================

    private JPanel buildIssueTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Booking reference:"));
        north.add(txtReference);
        JButton btnFind = new JButton("Find");
        north.add(btnFind);
        panel.add(north, BorderLayout.NORTH);

        txtBooking.setEditable(false);
        txtBooking.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        panel.add(new JScrollPane(txtBooking), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPrint = new JButton("Print tickets");
        south.add(btnPrint);
        panel.add(south, BorderLayout.SOUTH);

        btnFind.addActionListener(e -> find());
        txtReference.addActionListener(e -> find()); // Enter key
        btnPrint.addActionListener(e -> {
            if (foundBooking == null) {
                JOptionPane.showMessageDialog(this, "Find a booking first.");
            } else {
                printAll(foundBooking);
                find(); // refresh the "printed" status
            }
        });
        return panel;
    }

    private void find() {
        String ref = txtReference.getText().trim();
        if (ref.isEmpty()) return;
        try {
            foundBooking = bookingDAO.findByReference(ref);
            if (foundBooking == null) {
                txtBooking.setText("No booking found for " + ref.toUpperCase());
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(foundBooking.getReference()).append("   ").append(foundBooking.getStatus()).append("\n");
            sb.append(foundBooking.getShowtime().getMovie().getTitle()).append("   ")
              .append(foundBooking.getShowtime()).append("\n\n");
            for (Ticket t : foundBooking.getTickets()) {
                sb.append(String.format("%-4s %-5s Rs. %8.2f   %s%s%n", t.getSeat().getLabel(), t.getType(), t.getPrice(),
                        t.isPrinted() ? "printed" : "NOT printed", t.needsIdCheck() ? "   [check ID]" : ""));
            }
            if (foundBooking.getStatus() != BookingStatus.CONFIRMED) {
                sb.append("\nThis booking is not paid, so tickets cannot be printed.");
            }
            txtBooking.setText(sb.toString());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Prints every ticket of the booking that hasn't been printed yet (each ticket only once, R24). */
    private void printAll(Booking booking) {
        if (booking == null) return;
        StringBuilder out = new StringBuilder();
        int printed = 0;
        for (Ticket t : booking.getTickets()) {
            if (t.isPrinted()) continue;
            try {
                bookingDAO.markTicketPrinted(t.getTicketId(), cashier.getCashierId());
                cashier.printTicket(t);
                out.append(t.print()).append("\n\n");
                printed++;
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot print", JOptionPane.WARNING_MESSAGE);
                return;
            } catch (SQLException ex) {
                showError(ex);
                return;
            }
        }
        if (printed == 0) {
            JOptionPane.showMessageDialog(this, "All tickets for this booking were already printed.");
            return;
        }
        JTextArea area = new JTextArea(out.toString(), 18, 30);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), printed + " ticket(s) printed",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
