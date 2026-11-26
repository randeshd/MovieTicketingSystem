package com.movieticketing.model;

import javax.swing.JFrame;
import com.movieticketing.ui.ManagerDashboard;

public class Manager extends User {

    private int managerId; // movies.created_by and settings.updated_by point at this

    public Manager(int userId, String name, String email, String passwordHash, boolean active, int managerId) {
        super(userId, name, email, passwordHash, Role.MANAGER, active);
        this.managerId = managerId;
    }

    @Override
    public JFrame createDashboard() {
        return new ManagerDashboard(this);
    }

    public int getManagerId() { return managerId; }
    public void setManagerId(int managerId) { this.managerId = managerId; }
}