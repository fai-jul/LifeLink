package com.lifelink.model;

import java.time.LocalDateTime;

public class BloodRequest {

    private int id;
    private int recipientId;
    private BloodType bloodType;
    private int quantity;
    private String location;
    private double latitude;
    private double longitude;
    private RequestPriority urgency;
    private RequestStatus status;
    private LocalDateTime createdAt;

    public BloodRequest() {
    }

    public BloodRequest(int id, int recipientId, BloodType bloodType, int quantity,
                         String location, double latitude, double longitude,
                         RequestPriority urgency, RequestStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.bloodType = bloodType;
        this.quantity = quantity;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.urgency = urgency;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
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

    public RequestPriority getUrgency() {
        return urgency;
    }

    public void setUrgency(RequestPriority urgency) {
        this.urgency = urgency;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
