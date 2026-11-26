package com.movieticketing.ui;

import com.movieticketing.dao.UserDAO;
import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Customer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** A new customer creates their own account, then goes back to the login screen. */
public class RegisterFrame extends JFrame {

    private final JTextField txtName = new JTextField(22);
    private final JTextField txtEmail = new JTextField(22);
    private final JTextField txtPhone = new JTextField(22);
    private final JTextField txtNic = new JTextField(22);
    private final JTextField txtDob = new JTextField(22);
    private final JPasswordField txtPassword = new JPasswordField(22);
    private final JPasswordField txtConfirm = new JPasswordField(22);

    public RegisterFrame() {
        super("ABC Cinema - Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Create your account", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setBorder(BorderFactory.createEmptyBorder(16, 20, 6, 20));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(6, 30, 6, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;
        Object[][] rows = {
            {"Full name", txtName}, {"Email", txtEmail}, {"Phone", txtPhone}, {"NIC", txtNic},
            {"Date of birth (YYYY-MM-DD)", txtDob}, {"Password (8+ characters)", txtPassword},
            {"Confirm password", txtConfirm}
        };
        for (int i = 0; i < rows.length; i++) {
            c.gridy = i;
            c.gridx = 0; form.add(new JLabel((String) rows[i][0]), c);
            c.gridx = 1; form.add((JComponent) rows[i][1], c);
        }
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        JButton btnBack = new JButton("Back to login");
        JButton btnRegister = new JButton("Register");
        buttons.add(btnBack);
        buttons.add(btnRegister);
        add(buttons, BorderLayout.SOUTH);

        btnRegister.addActionListener(e -> register());
        btnBack.addActionListener(e -> { new LoginFrame().setVisible(true); dispose(); });
        getRootPane().setDefaultButton(btnRegister);

        pack();
        setLocationRelativeTo(null);
    }

    private void register() {
        try {
            String name = txtName.getText().trim();
            String email = txtEmail.getText().trim();
            String phone = txtPhone.getText().trim();
            String nic = txtNic.getText().trim().toUpperCase();
            String password = new String(txtPassword.getPassword());

            // Simple checks first, so the user gets a clear message instead of a database error.
            if (name.isEmpty() || phone.isEmpty() || nic.isEmpty()) {
                throw new ValidationException("Please fill in every field.");
            }
            if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                throw new ValidationException("Please enter a valid email address.");
            }
            if (!phone.matches("\\+?\\d{9,14}")) {
                throw new ValidationException("Phone must be digits only, for example 0771234567.");
            }
            // Sri Lankan NIC: old = 9 digits + V or X, new = 12 digits.
            if (!nic.matches("\\d{9}[VX]|\\d{12}")) {
                throw new ValidationException("NIC must be 9 digits + V/X, or 12 digits.");
            }
            LocalDate dob;
            try {
                dob = LocalDate.parse(txtDob.getText().trim());
            } catch (DateTimeParseException ex) {
                throw new ValidationException("Type your date of birth as YYYY-MM-DD, for example 2004-08-15.");
            }
            if (!dob.isBefore(LocalDate.now())) {
                throw new ValidationException("Date of birth must be in the past.");
            }
            if (!password.equals(new String(txtConfirm.getPassword()))) {
                throw new ValidationException("The two passwords don't match.");
            }

            // id 0 and hash null for now: the DAO hashes the password and fills in the new ids.
            Customer customer = new Customer(0, name, email, null, true, 0, phone, nic, dob);
            new UserDAO().registerCustomer(customer, password);   // checks length 8+, duplicate email/NIC

            JOptionPane.showMessageDialog(this, "Account created. You can log in now.");
            new LoginFrame(email).setVisible(true);
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check the details", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Cannot reach the database: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
