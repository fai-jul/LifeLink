package com.lifelink.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Donor extends User {

    private BloodType bloodType;
    private int age;
    private double weight;
    private LocalDate lastDonationDate; // nullable: null means "never donated"
    private boolean available;

    public Donor() {
        super();
        setRole(UserRole.DONOR);
    }

    public Donor(int id, String name, String email, String passwordHash, String phone,
                 String location, double latitude, double longitude, LocalDateTime createdAt,
                 BloodType bloodType, int age, double weight, LocalDate lastDonationDate,
                 boolean available) {
        super(id, name, email, passwordHash, phone, UserRole.DONOR, location, latitude, longitude, createdAt);
        this.bloodType = bloodType;
        this.age = age;
        this.weight = weight;
        this.lastDonationDate = lastDonationDate;
        this.available = available;
    }

    @Override
    public String getDashboardTitle() {
        return "Donor Dashboard";
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
