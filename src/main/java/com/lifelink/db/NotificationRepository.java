package com.lifelink.db;

import com.lifelink.model.Notification;
import com.lifelink.model.NotificationType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    private final Connection conn = DatabaseManager.getConnection();

    public synchronized void create(int userId, String message, NotificationType type) {
        String sql = "INSERT INTO notifications (user_id, message, type, read, created_at) VALUES (?, ?, ?, 0, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, type.name());
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    public List<Notification> findForUser(int userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC";
        List<Notification> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("message"),
                            NotificationType.valueOf(rs.getString("type")),
                            rs.getInt("read") == 1,
                            LocalDateTime.parse(rs.getString("created_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notifications", e);
        }
        return list;
    }

    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count unread notifications", e);
        }
    }

    public void markAllRead(int userId) {
        String sql = "UPDATE notifications SET read = 1 WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark notifications read", e);
        }
    }
}
