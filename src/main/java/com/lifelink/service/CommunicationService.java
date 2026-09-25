package com.lifelink.service;

import com.lifelink.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Queues email/SMS work for a provider integration without losing delivery auditability. */
public class CommunicationService {

    private final Connection conn = DatabaseManager.getConnection();

    public void queueEmail(int userId, String destination, String subject, String message) {
        queue(userId, "EMAIL", destination, subject, message);
    }

    public void queueSms(int userId, String destination, String message) {
        queue(userId, "SMS", destination, null, message);
    }

    private void queue(int userId, String channel, String destination, String subject, String message) {
        String sql = "INSERT INTO outbound_notifications "
                + "(user_id, channel, destination, subject, message, status, created_at) VALUES (?, ?, ?, ?, ?, 'QUEUED', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, channel);
            ps.setString(3, destination);
            ps.setString(4, subject);
            ps.setString(5, message);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to queue " + channel + " notification", e);
        }
    }
}