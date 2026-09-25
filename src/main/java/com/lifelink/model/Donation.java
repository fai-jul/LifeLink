package com.lifelink.model;

import java.time.LocalDate;

public class Donation {

    private int id;
    private int donorId;
    private LocalDate donationDate;
    private BloodType bloodType;
    private int quantity;

    public Donation() {
    }

    public Donation(int id, int donorId, LocalDate donationDate, BloodType bloodType, int quantity) {
        this.id = id;
        this.donorId = donorId;
        this.donationDate = donationDate;
        this.bloodType = bloodType;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
