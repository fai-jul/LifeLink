package com.lifelink.service;

import com.lifelink.model.User;
import com.lifelink.model.UserRole;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class AccessControlService {

    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSIONS = Map.of(
            UserRole.DONOR, EnumSet.of(Permission.MANAGE_DONORS),
            UserRole.RECIPIENT, EnumSet.of(Permission.MANAGE_REQUESTS),
            UserRole.BLOOD_BANK, EnumSet.of(Permission.MANAGE_INVENTORY, Permission.MANAGE_REQUESTS,
                    Permission.DISPATCH_TRANSFERS, Permission.VIEW_REPORTS),
            UserRole.DOCTOR, EnumSet.of(Permission.MANAGE_DONORS, Permission.MANAGE_REQUESTS,
                    Permission.PERFORM_CROSSMATCH, Permission.VIEW_REPORTS),
            UserRole.LAB_TECHNICIAN, EnumSet.of(Permission.SCREEN_DONATIONS, Permission.PERFORM_CROSSMATCH,
                    Permission.MANAGE_INVENTORY),
            UserRole.ADMINISTRATOR, EnumSet.allOf(Permission.class),
            UserRole.RECEPTIONIST, EnumSet.of(Permission.MANAGE_DONORS, Permission.MANAGE_REQUESTS,
                    Permission.VIEW_REPORTS)
    );

    public boolean can(User user, Permission permission) {
        return user != null && ROLE_PERMISSIONS.getOrDefault(user.getRole(), Set.of()).contains(permission);
    }

    public void require(User user, Permission permission) {
        if (!can(user, permission)) {
            throw new SecurityException("Role " + (user == null ? "UNKNOWN" : user.getRole())
                    + " is not allowed to perform " + permission);
        }
    }
}