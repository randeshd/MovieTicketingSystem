package com.movieticketing.ui;

import com.movieticketing.dao.MovieDAO;
import com.movieticketing.dao.ReportDAO;
import com.movieticketing.dao.SettingDAO;
import com.movieticketing.dao.ShowtimeDAO;
import com.movieticketing.dao.UserDAO;
import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Cashier;
import com.movieticketing.model.Manager;
import com.movieticketing.model.Movie;
import com.movieticketing.model.MovieStatus;
import com.movieticketing.model.Role;
import com.movieticketing.model.Setting;
import com.movieticketing.model.Showtime;
import com.movieticketing.model.User;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** The manager runs the cinema: movies, shows, staff, the Half %, and reports. */
public class ManagerDashboard extends JFrame {

    private final Manager manager;
    private final MovieDAO movieDAO = new MovieDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SettingDAO settingDAO = new SettingDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    public ManagerDashboard(Manager manager) {
        super("ABC Cinema - Manager: " + manager.getName());
        this.manager = manager;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        top.add(new JLabel("Manager: " + manager.getName()), BorderLayout.WEST);
        JButton btnLogout = new JButton("Logout");
        top.add(btnLogout, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        btnLogout.addActionListener(e -> { new LoginFrame().setVisible(true); dispose(); });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Movies", new MoviesTab());
        tabs.addTab("Showtimes", new ShowtimesTab());
        tabs.addTab("Staff", new StaffTab());
        tabs.addTab("Settings", buildSettingsTab());
        tabs.addTab("Reports", buildReportsTab());
        add(tabs, BorderLayout.CENTER);

        setSize(1000, 620);
        setLocationRelativeTo(null);
    }

    // ===================== helpers used by every tab =====================

    /** A table whose cells can't be typed into. */
    private static JTable readOnlyTable(DefaultTableModel model) {
        JTable t = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    /** Puts label/field pairs into a tidy 2-column form. */
    private static JPanel form(Object... labelFieldPairs) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labelFieldPairs.length; i += 2) {
            c.gridy = i / 2;
            c.gridx = 0; p.add(new JLabel((String) labelFieldPairs[i]), c);
            c.gridx = 1; p.add((JComponent) labelFieldPairs[i + 1], c);
        }
        return p;
    }

    private void showError(Exception ex) {
        if (ex instanceof ValidationException) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check the details", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static LocalDate parseDate(String text) throws ValidationException {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Type the date as YYYY-MM-DD, for example 2026-12-05.");
        }
    }

    // ===================== MOVIES TAB =====================
    // INNER CLASS: each tab is its own small class inside this one, and can use the DAOs above.

    private class MoviesTab extends JPanel {
        private final DefaultTableModel model =
                new DefaultTableModel(new String[] {"ID", "Title", "Genre", "Mins", "18+", "Price", "Status"}, 0);
        private final JTable table = readOnlyTable(model);
        private List<Movie> movies;

        private final JTextField txtTitle = new JTextField(20);
        private final JTextField txtGenre = new JTextField(20);
        private final JTextField txtMinutes = new JTextField(6);
        private final JTextField txtPrice = new JTextField(8);
        private final JCheckBox chk18 = new JCheckBox("18+ only");
        private final JComboBox<MovieStatus> cmbStatus = new JComboBox<>(MovieStatus.values());
        private final JTextField txtTrailer = new JTextField(20);
        private final JTextArea txtDescription = new JTextArea(3, 20);

        MoviesTab() {
            super(new BorderLayout(8, 8));
            add(new JScrollPane(table), BorderLayout.CENTER);

            txtDescription.setLineWrap(true);
            JPanel right = new JPanel(new BorderLayout());
            right.setBorder(BorderFactory.createTitledBorder("Movie details"));
            right.add(form("Title", txtTitle, "Genre", txtGenre, "Minutes", txtMinutes, "Full price (Rs.)", txtPrice,
                    "", chk18, "Status", cmbStatus, "Trailer URL", txtTrailer,
                    "Description", new JScrollPane(txtDescription)), BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout());
            JButton btnAdd = new JButton("Add new");
            JButton btnUpdate = new JButton("Update selected");
            JButton btnClear = new JButton("Clear");
            buttons.add(btnAdd);
            buttons.add(btnUpdate);
            buttons.add(btnClear);
            right.add(buttons, BorderLayout.SOUTH);
            add(right, BorderLayout.EAST);

            table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) fillForm(); });
            btnAdd.addActionListener(e -> save(true));
            btnUpdate.addActionListener(e -> save(false));
            btnClear.addActionListener(e -> { table.clearSelection(); clearForm(); });
            load();
        }

        void load() {
            try {
                movies = movieDAO.findAll();
                model.setRowCount(0);
                for (Movie m : movies) {
                    model.addRow(new Object[] {m.getMovieId(), m.getTitle(), m.getGenre(), m.getDurationMin(),
                            m.is18Plus() ? "Yes" : "", String.format("%.2f", m.getBasePrice()), m.getStatus()});
                }
            } catch (SQLException ex) {
                showError(ex);
            }
        }

        private void fillForm() {
            int row = table.getSelectedRow();
            if (row < 0) return;
            Movie m = movies.get(row);
            txtTitle.setText(m.getTitle());
            txtGenre.setText(m.getGenre());
            txtMinutes.setText(String.valueOf(m.getDurationMin()));
            txtPrice.setText(String.valueOf(m.getBasePrice()));
            chk18.setSelected(m.is18Plus());
            cmbStatus.setSelectedItem(m.getStatus());
            txtTrailer.setText(m.getTrailerUrl() == null ? "" : m.getTrailerUrl());
            txtDescription.setText(m.getDescription() == null ? "" : m.getDescription());
        }

        private void clearForm() {
            for (JTextField f : new JTextField[] {txtTitle, txtGenre, txtMinutes, txtPrice, txtTrailer}) f.setText("");
            txtDescription.setText("");
            chk18.setSelected(false);
            cmbStatus.setSelectedItem(MovieStatus.COMING_SOON);
        }

        private void save(boolean isNew) {
            int row = table.getSelectedRow();
            if (!isNew && row < 0) {
                JOptionPane.showMessageDialog(ManagerDashboard.this, "Select a movie in the table first.");
                return;
            }
            try {
                Movie m = isNew ? new Movie() : movies.get(row);
                m.setTitle(txtTitle.getText());
                m.setGenre(txtGenre.getText());
                try {
                    m.setDurationMin(Integer.parseInt(txtMinutes.getText().trim()));
                    m.setBasePrice(Double.parseDouble(txtPrice.getText().trim()));
                } catch (NumberFormatException e) {
                    throw new ValidationException("Minutes and price must be numbers.");
                }
                m.set18Plus(chk18.isSelected());
                m.setStatus((MovieStatus) cmbStatus.getSelectedItem());
                m.setTrailerUrl(txtTrailer.getText().isBlank() ? null : txtTrailer.getText().trim());
                m.setDescription(txtDescription.getText().isBlank() ? null : txtDescription.getText().trim());
                if (isNew) movieDAO.add(m, manager.getManagerId()); else movieDAO.update(m);
                JOptionPane.showMessageDialog(ManagerDashboard.this, isNew ? "Movie added." : "Movie updated.");
                load();
                clearForm();
            } catch (ValidationException | SQLException ex) {
                showError(ex);
            }
        }
    }

    // ===================== SHOWTIMES TAB =====================

    private class ShowtimesTab extends JPanel {
        private final DefaultTableModel model =
                new DefaultTableModel(new String[] {"ID", "Movie", "Starts", "Ends", "Sold / 40"}, 0);
        private final JTable table = readOnlyTable(model);
        private final JTextField txtDate = new JTextField(LocalDate.now().toString(), 10);
        private final JComboBox<Movie> cmbMovie = new JComboBox<>();
        private final JTextField txtTime = new JTextField("18:00", 6);

        ShowtimesTab() {
            super(new BorderLayout(8, 8));
            JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
            north.add(new JLabel("Date (YYYY-MM-DD):"));
            north.add(txtDate);
            JButton btnLoad = new JButton("Show");
            north.add(btnLoad);
            add(north, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
            south.setBorder(BorderFactory.createTitledBorder("Add a show on the date above"));
            south.add(new JLabel("Movie:"));
            south.add(cmbMovie);
            south.add(new JLabel("Start (HH:MM):"));
            south.add(txtTime);
            JButton btnAdd = new JButton("Add show");
            south.add(btnAdd);
            add(south, BorderLayout.SOUTH);

            btnLoad.addActionListener(e -> load());
            btnAdd.addActionListener(e -> addShow());
            loadMovies();
            load();
        }

        private void loadMovies() {
            try {
                List<Movie> list = movieDAO.findAll();
                list.removeIf(m -> m.getStatus() == MovieStatus.ARCHIVED);
                cmbMovie.setModel(new DefaultComboBoxModel<>(list.toArray(new Movie[0])));
            } catch (SQLException ex) {
                showError(ex);
            }
        }

        private void load() {
            try {
                model.setRowCount(0);
                for (Showtime s : showtimeDAO.findByDate(parseDate(txtDate.getText()))) {
                    model.addRow(new Object[] {s.getShowtimeId(), s.getMovie().getTitle(), s.getShowTime(),
                            s.getEndTime(), reportDAO.occupancy(s.getShowtimeId())});
                }
            } catch (ValidationException | SQLException ex) {
                showError(ex);
            }
        }

        private void addShow() {
            try {
                Showtime s = new Showtime();
                s.setMovie((Movie) cmbMovie.getSelectedItem());
                s.setShowDate(parseDate(txtDate.getText()));
                try {
                    s.setShowTime(LocalTime.parse(txtTime.getText().trim()));
                } catch (DateTimeParseException e) {
                    throw new ValidationException("Type the time as HH:MM, for example 18:30.");
                }
                showtimeDAO.add(s, manager.getManagerId());
                JOptionPane.showMessageDialog(ManagerDashboard.this, "Show added: " + s.getMovie().getTitle() + " at " + s);
                load();
            } catch (ValidationException | SQLException ex) {
                showError(ex);
            }
        }
    }

    // ===================== STAFF TAB =====================

    private class StaffTab extends JPanel {
        private final DefaultTableModel model =
                new DefaultTableModel(new String[] {"User ID", "Name", "Email", "Role", "Status"}, 0);
        private final JTable table = readOnlyTable(model);
        private final JTextField txtName = new JTextField(18);
        private final JTextField txtEmail = new JTextField(18);
        private final JPasswordField txtPassword = new JPasswordField(18);
        private final JComboBox<Role> cmbRole = new JComboBox<>(new Role[] {Role.CASHIER, Role.MANAGER});

        StaffTab() {
            super(new BorderLayout(8, 8));
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel right = new JPanel(new BorderLayout());
            right.setBorder(BorderFactory.createTitledBorder("Add staff member"));
            right.add(form("Name", txtName, "Email", txtEmail, "Password (8+)", txtPassword, "Role", cmbRole),
                    BorderLayout.NORTH);
            JButton btnAdd = new JButton("Add");
            JPanel addRow = new JPanel(new FlowLayout());
            addRow.add(btnAdd);
            right.add(addRow, BorderLayout.CENTER);
            add(right, BorderLayout.EAST);

            JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton btnToggle = new JButton("Activate / deactivate selected");
            south.add(btnToggle);
            add(south, BorderLayout.SOUTH);

            btnAdd.addActionListener(e -> addStaff());
            btnToggle.addActionListener(e -> toggle());
            load();
        }

        private void load() {
            try {
                model.setRowCount(0);
                for (String[] row : userDAO.findStaff()) model.addRow(row);
            } catch (SQLException ex) {
                showError(ex);
            }
        }

        private void addStaff() {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            if (name.isEmpty() || !email.contains("@")) {
                JOptionPane.showMessageDialog(ManagerDashboard.this, "Enter a name and a valid email.");
                return;
            }
            // POLYMORPHISM: addStaff() takes any User; we build the right subclass here.
            User user = cmbRole.getSelectedItem() == Role.CASHIER
                    ? new Cashier(0, name, email, null, true, 0)
                    : new Manager(0, name, email, null, true, 0);
            try {
                userDAO.addStaff(user, new String(txtPassword.getPassword()));
                JOptionPane.showMessageDialog(ManagerDashboard.this, name + " added as " + user.getRole() + ".");
                txtName.setText("");
                txtEmail.setText("");
                txtPassword.setText("");
                load();
            } catch (ValidationException | SQLException ex) {
                showError(ex);
            }
        }

        private void toggle() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(ManagerDashboard.this, "Select a staff member first.");
                return;
            }
            int userId = Integer.parseInt((String) model.getValueAt(row, 0));
            if (userId == manager.getUserId()) {
                JOptionPane.showMessageDialog(ManagerDashboard.this, "You can't deactivate yourself.");
                return;
            }
            boolean nowActive = "Active".equals(model.getValueAt(row, 4));
            try {
                userDAO.setActive(userId, !nowActive);
                load();
            } catch (SQLException ex) {
                showError(ex);
            }
        }
    }

    // ===================== SETTINGS TAB =====================

    private JPanel buildSettingsTab() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 20));
        JTextField txtHalf = new JTextField(5);
        try {
            txtHalf.setText(String.valueOf(settingDAO.getHalfTicketPercent()));
        } catch (SQLException ex) {
            showError(ex);
        }
        JButton btnSave = new JButton("Save");
        panel.add(new JLabel("Half ticket price = this % of the Full price:"));
        panel.add(txtHalf);
        panel.add(btnSave);
        btnSave.addActionListener(e -> {
            try {
                settingDAO.update(Setting.HALF_TICKET_PERCENT, txtHalf.getText(), manager.getManagerId());
                JOptionPane.showMessageDialog(this, "Saved. New bookings use " + txtHalf.getText().trim() + "%.");
            } catch (ValidationException | SQLException ex) {
                showError(ex);
            }
        });
        return panel;
    }

    // ===================== REPORTS TAB =====================

    private JPanel buildReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        // Daily sales (left)
        JPanel daily = new JPanel(new BorderLayout(5, 5));
        daily.setBorder(BorderFactory.createTitledBorder("Daily sales"));
        JTextField txtDay = new JTextField(LocalDate.now().toString(), 10);
        JButton btnDay = new JButton("Show");
        JPanel dayRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dayRow.add(new JLabel("Date:"));
        dayRow.add(txtDay);
        dayRow.add(btnDay);
        JTextArea txtDaily = new JTextArea(10, 32);
        txtDaily.setEditable(false);
        txtDaily.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        daily.add(dayRow, BorderLayout.NORTH);
        daily.add(new JScrollPane(txtDaily), BorderLayout.CENTER);
        panel.add(daily, BorderLayout.WEST);

        // Sales per movie (right)
        JPanel perMovie = new JPanel(new BorderLayout(5, 5));
        perMovie.setBorder(BorderFactory.createTitledBorder("Sales per movie (by show date)"));
        JTextField txtFrom = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
        JTextField txtTo = new JTextField(LocalDate.now().plusMonths(2).toString(), 10);
        JButton btnRange = new JButton("Show");
        JPanel rangeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rangeRow.add(new JLabel("From:"));
        rangeRow.add(txtFrom);
        rangeRow.add(new JLabel("To:"));
        rangeRow.add(txtTo);
        rangeRow.add(btnRange);
        DefaultTableModel model = new DefaultTableModel(new String[] {"Movie", "Tickets", "Revenue (Rs.)"}, 0);
        perMovie.add(rangeRow, BorderLayout.NORTH);
        perMovie.add(new JScrollPane(readOnlyTable(model)), BorderLayout.CENTER);
        panel.add(perMovie, BorderLayout.CENTER);

        btnDay.addActionListener(e -> {
            try {
                txtDaily.setText(reportDAO.dailySales(parseDate(txtDay.getText())).print()); // Printable
            } catch (ValidationException | SQLException ex) {
                showError(ex);
            }
        });
        btnRange.addActionListener(e -> {
            try {
                model.setRowCount(0);
                for (String[] row : reportDAO.salesPerMovie(parseDate(txtFrom.getText()), parseDate(txtTo.getText()))) {
                    model.addRow(row);
                }
            } catch (ValidationException | SQLException ex) {
                showError(ex);
            }
        });
        return panel;
    }
}
