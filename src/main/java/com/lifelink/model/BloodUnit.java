package com.lifelink.model;

import java.time.LocalDate;

public class BloodUnit {

    private int id;
    private int bloodBankId;
    private BloodType bloodType;
    private BloodComponent component;
    private int quantity; // number of units (bags)
    private String storageLocation;
    private LocalDate collectionDate;
    private LocalDate expiryDate;
    private BloodUnitStatus status;

    public BloodUnit() {
    }

    public BloodUnit(int id, int bloodBankId, BloodType bloodType, int quantity,
                      LocalDate collectionDate, LocalDate expiryDate, BloodUnitStatus status) {
        this(id, bloodBankId, bloodType, BloodComponent.WHOLE_BLOOD, quantity,
            collectionDate, expiryDate, status, "Unassigned");
        }

        public BloodUnit(int id, int bloodBankId, BloodType bloodType, BloodComponent component, int quantity,
                  LocalDate collectionDate, LocalDate expiryDate, BloodUnitStatus status,
                  String storageLocation) {
        this.id = id;
        this.bloodBankId = bloodBankId;
        this.bloodType = bloodType;
        this.component = component;
        this.quantity = quantity;
        this.collectionDate = collectionDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.storageLocation = storageLocation;
    }

    /** Whole blood typically has a shelf life of ~42 days from collection. */
    public static LocalDate defaultExpiryFrom(LocalDate collectionDate) {
        return collectionDate.plusDays(42);
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    public boolean isExpiringSoon(int withinDays) {
        if (expiryDate == null) return false;
        LocalDate threshold = LocalDate.now().plusDays(withinDays);
        return !isExpired() && !expiryDate.isAfter(threshold);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(int bloodBankId) {
        this.bloodBankId = bloodBankId;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public BloodComponent getComponent() {
        return component;
    }

    public void setComponent(BloodComponent component) {
        this.component = component;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BloodUnitStatus getStatus() {
        return status;
    }

    public void setStatus(BloodUnitStatus status) {
        this.status = status;
    }
}
