package com.movieticketing.ui;

import com.movieticketing.dao.UserDAO;
import com.movieticketing.exception.AuthenticationException;
import com.movieticketing.model.User;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** INHERITANCE: LoginFrame IS-A JFrame, so it gets all window behaviour for free. */
public class LoginFrame extends JFrame {

    private final JTextField txtEmail = new JTextField(22);
    private final JPasswordField txtPassword = new JPasswordField(22);
    private final JButton btnLogin = new JButton("Login");
    private final JButton btnRegister = new JButton("New customer? Register");

    /** Used after registering: opens the login screen with the new email already typed in. */
    public LoginFrame(String email) {
        this();
        txtEmail.setText(email);
        txtPassword.requestFocusInWindow();
    }

    public LoginFrame() {
        super("ABC Cinema - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("ABC Cinema", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        add(title, BorderLayout.NORTH);

        // GridBagLayout = a grid where we say which column/row each item goes in.
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Email"), c);
        c.gridx = 1;              form.add(txtEmail, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Password"), c);
        c.gridx = 1;              form.add(txtPassword, c);
        c.gridx = 1; c.gridy = 2; c.anchor = GridBagConstraints.EAST;
        form.add(btnLogin, c);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.anchor = GridBagConstraints.CENTER;
        form.add(btnRegister, c);
        add(form, BorderLayout.CENTER);

        // EVENT HANDLING: a lambda runs when the button is clicked (Enter works too).
        btnLogin.addActionListener(e -> login());
        getRootPane().setDefaultButton(btnLogin);
        btnRegister.addActionListener(e -> { new RegisterFrame().setVisible(true); dispose(); });

        pack();                       // size the window to fit its contents
        setLocationRelativeTo(null);  // centre of the screen
    }

    private void login() {
        String email = txtEmail.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your email and password.");
            return;
        }
        try {
            User user = new UserDAO().login(email, password);
            // POLYMORPHISM: we don't know (or care) if it's a Customer, Cashier or Manager.
            JFrame dashboard = user.createDashboard();
            if (dashboard == null) { // dashboards not built yet
                JOptionPane.showMessageDialog(this, "Welcome " + user.getName() + " (" + user.getRole() + ")");
            } else {
                dashboard.setVisible(true);
                dispose();
            }
        } catch (AuthenticationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login failed", JOptionPane.WARNING_MESSAGE);
            txtPassword.setText("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Cannot reach the database: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
