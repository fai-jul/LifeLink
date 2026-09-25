package com.lifelink.db;

import com.lifelink.model.ActivityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogRepository {

    private final Connection conn = DatabaseManager.getConnection();

    public synchronized void log(int userId, String action, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action, description, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, description);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to write activity log", e);
        }
    }

    public List<ActivityLog> recent(int limit) {
        String sql = "SELECT * FROM activity_logs ORDER BY id DESC LIMIT ?";
        List<ActivityLog> logs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("action"),
                            rs.getString("description"),
                            LocalDateTime.parse(rs.getString("timestamp"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch activity logs", e);
        }
        return logs;
    }

    public List<ActivityLog> forUser(int userId, int limit) {
        String sql = "SELECT * FROM activity_logs WHERE user_id = ? ORDER BY id DESC LIMIT ?";
        List<ActivityLog> logs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLog(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("action"),
                            rs.getString("description"),
                            LocalDateTime.parse(rs.getString("timestamp"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user activity logs", e);
        }
        return logs;
    }
}
