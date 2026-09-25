package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * Abstract base for all account types. Holds fields common to every
 * user regardless of role; role-specific data lives in Donor/Recipient.
 * BloodBank is modelled as a separate entity (see BloodBank.java) since
 * it represents an organization rather than an individual, but it still
 * has an associated User row for login credentials.
 */
public abstract class User {

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private UserRole role;
    private String location;
    private double latitude;
    private double longitude;
    private LocalDateTime createdAt;

    protected User() {
    }

    protected User(int id, String name, String email, String passwordHash, String phone,
                   UserRole role, String location, double latitude, double longitude,
                   LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }

    /** Each subclass returns a short label used in the dashboard header. */
    public abstract String getDashboardTitle();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
