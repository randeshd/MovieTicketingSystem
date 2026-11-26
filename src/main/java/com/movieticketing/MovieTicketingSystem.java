package com.movieticketing;

import com.movieticketing.ui.LoginFrame;
import javax.swing.UIManager;

public class MovieTicketingSystem {
    public static void main(String[] args) {
        try {
            // Same look on Mac and Windows, and it lets us colour the seat buttons.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }
        java.awt.EventQueue.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
