package com.movieticketing;

import com.movieticketing.dao.UserDAO;
import com.movieticketing.model.User;

public class MovieTicketingSystem {
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        String[][] tests = {
            {"manager@abccinema.lk", "Password@123"},
            {"amaya@gmail.com",      "Password@123"},
            {"saman@abccinema.lk",   "Password@123"},
            {"amaya@gmail.com",      "wrongpass"},
            {"ruwan@abccinema.lk",   "Password@123"}
        };
        for (String[] t : tests) {
            try {
                User u = dao.login(t[0], t[1]);
                System.out.println("OK   " + t[0] + " -> " + u.getClass().getSimpleName() + " " + u.getName());
            } catch (Exception e) {
                System.out.println("FAIL " + t[0] + " -> " + e.getMessage());
            }
        }
    }
}