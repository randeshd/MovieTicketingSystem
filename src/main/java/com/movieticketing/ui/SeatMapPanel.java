package com.movieticketing.ui;

import com.movieticketing.model.Seat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * The 40-seat hall as clickable buttons. REUSE: the customer AND the cashier screens
 * both put one of these inside them (COMPOSITION: a screen HAS-A SeatMapPanel).
 */
public class SeatMapPanel extends JPanel {

    private static final Color TAKEN = new Color(200, 80, 80);
    private static final Color CHOSEN = new Color(70, 160, 90);

    private final JPanel grid = new JPanel(new GridLayout(5, 8, 6, 6));
    private final Map<JButton, Seat> buttons = new LinkedHashMap<>();
    private final Set<JButton> chosen = new LinkedHashSet<>();
    private Runnable onChange = () -> { };

    public SeatMapPanel() {
        super(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel screen = new JLabel("SCREEN", SwingConstants.CENTER);
        screen.setOpaque(true);
        screen.setBackground(Color.LIGHT_GRAY);
        add(screen, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
        add(new JLabel("Green = your choice    Red = taken", SwingConstants.CENTER), BorderLayout.SOUTH);
    }

    /** Called whenever a seat is clicked (the screen uses it to update the total). */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Rebuild the map for one show. */
    public void load(List<Seat> seats, List<Integer> takenSeatIds) {
        grid.removeAll();
        buttons.clear();
        chosen.clear();
        for (Seat seat : seats) {
            JButton b = new JButton(seat.getLabel());
            b.setOpaque(true);
            if (takenSeatIds.contains(seat.getSeatId())) {
                b.setEnabled(false);
                b.setBackground(TAKEN);
            }
            b.addActionListener(e -> {
                if (chosen.remove(b)) {         // clicked again: un-choose it
                    b.setBackground(null);
                } else {
                    chosen.add(b);
                    b.setBackground(CHOSEN);
                }
                onChange.run();
            });
            buttons.put(b, seat);
            grid.add(b);
        }
        grid.revalidate();
        grid.repaint();
    }

    public void clear() {
        load(new ArrayList<>(), new ArrayList<>());
    }

    public List<Seat> getSelectedSeats() {
        List<Seat> list = new ArrayList<>();
        for (JButton b : chosen) list.add(buttons.get(b));
        return list;
    }
}
