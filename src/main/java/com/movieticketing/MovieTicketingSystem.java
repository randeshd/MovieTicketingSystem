package com.movieticketing;

import com.movieticketing.dao.ReportDAO;
import java.time.LocalDate;

public class MovieTicketingSystem {
    public static void main(String[] args) throws Exception {
        ReportDAO reports = new ReportDAO();
        System.out.println(reports.dailySales(LocalDate.of(2026, 11, 21)).print());
        System.out.println();
        System.out.println(reports.dailySales(LocalDate.now()).print());
        System.out.println();
        for (String[] row : reports.salesPerMovie(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 1, 31))) {
            System.out.println(row[0] + ": " + row[1] + " tickets, Rs. " + row[2]);
        }
        System.out.println("Show 6 occupancy: " + reports.occupancy(6) + " / 40");
    }
}
