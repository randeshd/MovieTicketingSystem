package com.movieticketing.model;

import com.movieticketing.util.PasswordUtil;
import javax.swing.JFrame;

public abstract class User {

    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean active;

    protected User(int userId, String name, String email, String passwordHash, Role role, boolean active) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public boolean checkPassword(String password) {
        return PasswordUtil.matches(password, passwordHash);
    }

    public abstract JFrame createDashboard();

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}