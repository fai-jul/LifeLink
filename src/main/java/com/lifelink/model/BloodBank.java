package com.lifelink.model;

import java.time.LocalDateTime;

/**
 * A blood bank / hospital account. Extends User so it can log in like
 * any other role, but represents an organization rather than a person.
 */
public class BloodBank extends User {

    private String address;

    public BloodBank() {
        super();
        setRole(UserRole.BLOOD_BANK);
    }

    public BloodBank(int id, String name, String email, String passwordHash, String phone,
                      String location, double latitude, double longitude, LocalDateTime createdAt,
                      String address) {
        super(id, name, email, passwordHash, phone, UserRole.BLOOD_BANK, location, latitude, longitude, createdAt);
        this.address = address;
    }

    @Override
    public String getDashboardTitle() {
        return "Blood Bank Dashboard";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
