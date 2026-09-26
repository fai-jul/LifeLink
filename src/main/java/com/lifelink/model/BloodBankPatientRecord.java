package com.lifelink.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BloodBankPatientRecord {

    private int id;
    private int bloodBankId;
    private String name;
    private String bloodType;
    private String location;
    private String phone;
    private LocalDate caseDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BloodBankPatientRecord() {
    }

    public BloodBankPatientRecord(int id, String name, String bloodType, String location, String phone,
                                LocalDate caseDate, String status) {
        this.id = id;
        this.name = name;
        this.bloodType = bloodType;
        this.location = location;
        this.phone = phone;
        this.caseDate = caseDate;
        this.status = status;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getCaseDate() {
        return caseDate;
    }

    public void setCaseDate(LocalDate caseDate) {
        this.caseDate = caseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
