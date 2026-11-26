package com.movieticketing.dao;

import com.movieticketing.exception.AuthenticationException;
import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Cashier;
import com.movieticketing.model.Customer;
import com.movieticketing.model.Manager;
import com.movieticketing.model.Role;
import com.movieticketing.model.User;
import com.movieticketing.util.PasswordUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {

    /** One query: the user row plus whichever subclass row exists (LEFT JOIN). */
    private static final String LOGIN_SQL =
        "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.is_active, "
      + "       c.customer_id, c.phone, c.nic, c.date_of_birth, "
      + "       ca.cashier_id, m.manager_id "
      + "FROM users u "
      + "LEFT JOIN customers c  ON c.user_id  = u.user_id "
      + "LEFT JOIN cashiers  ca ON ca.user_id = u.user_id "
      + "LEFT JOIN managers  m  ON m.user_id  = u.user_id "
      + "WHERE u.email = ?";

    /**
     * Returns a Customer, Cashier or Manager (POLYMORPHISM: the caller only sees User).
     * Same message for "no such email" and "wrong password", so nobody can guess emails.
     */
    public User login(String email, String password) throws AuthenticationException, SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(LOGIN_SQL)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PasswordUtil.matches(password, rs.getString("password_hash"))) {
                    throw new AuthenticationException("Wrong email or password.");
                }
                if (!rs.getBoolean("is_active")) {
                    throw new AuthenticationException("This account has been deactivated. Please contact the manager.");
                }
                int userId = rs.getInt("user_id");
                String name = rs.getString("name");
                String mail = rs.getString("email");
                String hash = rs.getString("password_hash");
                Role role = Role.valueOf(rs.getString("role"));
                switch (role) {
                    case CUSTOMER:
                        return new Customer(userId, name, mail, hash, true, rs.getInt("customer_id"),
                                rs.getString("phone"), rs.getString("nic"),
                                rs.getDate("date_of_birth").toLocalDate());
                    case CASHIER:
                        return new Cashier(userId, name, mail, hash, true, rs.getInt("cashier_id"));
                    default:
                        return new Manager(userId, name, mail, hash, true, rs.getInt("manager_id"));
                }
            }
        }
    }

    /** Two INSERTs (users, then customers) in ONE transaction: both happen or neither does. */
    public void registerCustomer(Customer customer, String password) throws ValidationException, SQLException {
        checkPassword(password);
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId = insertUser(con, customer.getName(), customer.getEmail(), password, Role.CUSTOMER);
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO customers (user_id, nic, phone, date_of_birth) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setString(2, customer.getNic());
                    ps.setString(3, customer.getPhone());
                    ps.setDate(4, Date.valueOf(customer.getDateOfBirth()));
                    ps.executeUpdate();
                    customer.setCustomerId(generatedId(ps));
                }
                con.commit();
                customer.setUserId(userId);
            } catch (SQLException e) {
                con.rollback();
                if (e.getErrorCode() == 1062) { // duplicate email or NIC
                    throw new ValidationException("That email or NIC is already registered.");
                }
                throw e;
            }
        }
    }

    /** Manager adds a cashier or another manager. */
    public void addStaff(User user, String password) throws ValidationException, SQLException {
        if (user.getRole() == Role.CUSTOMER) {
            throw new ValidationException("Use registerCustomer for customers.");
        }
        checkPassword(password);
        String table = user.getRole() == Role.CASHIER ? "cashiers" : "managers";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId = insertUser(con, user.getName(), user.getEmail(), password, user.getRole());
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO " + table + " (user_id) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                    int subId = generatedId(ps);
                    if (user instanceof Cashier) ((Cashier) user).setCashierId(subId);
                    if (user instanceof Manager) ((Manager) user).setManagerId(subId);
                }
                con.commit();
                user.setUserId(userId);
            } catch (SQLException e) {
                con.rollback();
                if (e.getErrorCode() == 1062) {
                    throw new ValidationException("That email is already registered.");
                }
                throw e;
            }
        }
    }

    /** Deactivate instead of delete, so old sales stay in the reports (R31). */
    public void setActive(int userId, boolean active) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE users SET is_active = ? WHERE user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ---- private helpers ----

    private int insertUser(Connection con, String name, String email, String password, Role role) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email.trim());
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, role.name());
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    private int generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getInt(1);
        }
    }

    private void checkPassword(String password) throws ValidationException {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters.");
        }
    }
}
