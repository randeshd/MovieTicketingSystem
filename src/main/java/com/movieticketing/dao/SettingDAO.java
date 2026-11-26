package com.movieticketing.dao;

import com.movieticketing.exception.ValidationException;
import com.movieticketing.model.Setting;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingDAO {

    public String get(String key) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT setting_value FROM settings WHERE setting_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("setting_value") : null;
            }
        }
    }

    /** The Half ticket % (R10). Falls back to 50 if the row is missing. */
    public double getHalfTicketPercent() throws SQLException {
        String v = get(Setting.HALF_TICKET_PERCENT);
        return v == null ? 50.0 : Double.parseDouble(v);
    }

    public void update(String key, String value, int managerId) throws ValidationException, SQLException {
        if (Setting.HALF_TICKET_PERCENT.equals(key)) {
            double p;
            try {
                p = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new ValidationException("Enter a number, for example 50.");
            }
            if (p <= 0 || p > 100) throw new ValidationException("Half ticket % must be between 1 and 100.");
        }
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE settings SET setting_value = ?, updated_by = ? WHERE setting_key = ?")) {
            ps.setString(1, value.trim());
            ps.setInt(2, managerId);
            ps.setString(3, key);
            ps.executeUpdate();
        }
    }
}
