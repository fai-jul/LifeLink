package com.lifelink.model;

import java.time.LocalDateTime;

public class ActivityLog {

    private int id;
    private int userId; // 0/unknown allowed for system-generated events
    private String action;
    private String description;
    private LocalDateTime timestamp;

    public ActivityLog() {
    }

    public ActivityLog(int id, int userId, String action, String description, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
