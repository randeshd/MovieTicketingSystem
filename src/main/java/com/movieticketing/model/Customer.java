package com.movieticketing.model;

import java.time.LocalDate;
import java.time.Period;
import javax.swing.JFrame;
import com.movieticketing.ui.CustomerDashboard;

public class Customer extends User {

    private int customerId;
    private String phone;
    private String nic;
    private LocalDate dateOfBirth;

    public Customer(int userId, String name, String email, String passwordHash, boolean active,
                    int customerId, String phone, String nic, LocalDate dateOfBirth) {
        super(userId, name, email, passwordHash, Role.CUSTOMER, active);
        this.customerId = customerId;
        this.phone = phone;
        this.nic = nic;
        this.dateOfBirth = dateOfBirth;
    }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean canBook(Movie movie) {
        return !movie.is18Plus() || getAge() >= 18;
    }

    @Override
    public JFrame createDashboard() {
        return new CustomerDashboard(this);
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}