package com.lifelink.model;

import java.time.LocalDateTime;

public class Recipient extends User {

    public Recipient() {
        super();
        setRole(UserRole.RECIPIENT);
    }

    public Recipient(int id, String name, String email, String passwordHash, String phone,
                      String location, double latitude, double longitude, LocalDateTime createdAt) {
        super(id, name, email, passwordHash, phone, UserRole.RECIPIENT, location, latitude, longitude, createdAt);
    }

    @Override
    public String getDashboardTitle() {
        return "Recipient Dashboard";
    }
}
