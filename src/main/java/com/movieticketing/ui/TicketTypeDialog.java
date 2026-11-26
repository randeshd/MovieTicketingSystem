package com.movieticketing.ui;

import com.movieticketing.model.Seat;
import com.movieticketing.model.TicketType;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Asks Full or Half for each chosen seat. Returns null if cancelled. */
public final class TicketTypeDialog {

    private TicketTypeDialog() { }

    public static List<TicketType> ask(Component parent, List<Seat> seats, boolean is18Plus) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));
        List<JComboBox<TicketType>> boxes = new ArrayList<>();
        for (Seat s : seats) {
            JComboBox<TicketType> box = is18Plus
                    ? new JComboBox<>(new TicketType[] { TicketType.FULL })   // no Half for 18+ (R11)
                    : new JComboBox<>(TicketType.values());
            panel.add(new JLabel("Seat " + s.getLabel()));
            panel.add(box);
            boxes.add(box);
        }
        panel.add(new JLabel("Half = under 12"));
        int ok = JOptionPane.showConfirmDialog(parent, panel, "Full or Half?", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return null;
        List<TicketType> types = new ArrayList<>();
        for (JComboBox<TicketType> b : boxes) types.add((TicketType) b.getSelectedItem());
        return types;
    }
}
