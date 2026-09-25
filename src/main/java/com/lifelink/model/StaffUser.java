package com.lifelink.model;

import java.time.LocalDateTime;

/** Generic account representation for staff roles without role-specific fields yet. */
public class StaffUser extends User {

    public StaffUser(int id, String name, String email, String passwordHash, String phone,
                     UserRole role, String location, double latitude, double longitude,
                     LocalDateTime createdAt) {
        super(id, name, email, passwordHash, phone, role, location, latitude, longitude, createdAt);
    }

    @Override
    public String getDashboardTitle() {
        return getRole().name().replace('_', ' ');
    }
}