package com.movieticketing;

import com.movieticketing.ui.LoginFrame;

public class MovieTicketingSystem {
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}