package com.movieticketing.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    // Each teammate changes USER/PASSWORD to their own MySQL login.
    private static final String URL = "jdbc:mysql://localhost:3306/movie_ticketing";
    private static final String USER = "root";
    private static final String PASSWORD = "root123";

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}