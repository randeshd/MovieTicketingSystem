package com.movieticketing.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The ONE place that knows how to reach MySQL. Every DAO calls getConnection().
 * The login is NOT in the code: it is read from db.properties in the project folder
 * (next to pom.xml). That file is in .gitignore, so each teammate keeps their own
 * MySQL password on their own computer and it never goes to GitHub.
 * Copy db.properties.example to db.properties to make one.
 */
public final class DatabaseConnection {

    private static final String FILE = "db.properties";
    private static Properties props;   // loaded once, the first time we connect

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        Properties p = settings();
        return DriverManager.getConnection(p.getProperty("url"), p.getProperty("user"), p.getProperty("password"));
    }

    private static synchronized Properties settings() throws SQLException {
        if (props != null) return props;
        Path path = Path.of(FILE).toAbsolutePath();
        if (!Files.exists(path)) {
            throw new SQLException("Missing " + FILE + " at " + path
                    + ". Copy db.properties.example to db.properties and put in your MySQL user and password.");
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            throw new SQLException("Cannot read " + path + ": " + e.getMessage());
        }
        for (String key : new String[] {"url", "user", "password"}) {
            if (p.getProperty(key) == null) {
                throw new SQLException(FILE + " is missing the line: " + key + "=...");
            }
        }
        props = p;
        return props;
    }
}
