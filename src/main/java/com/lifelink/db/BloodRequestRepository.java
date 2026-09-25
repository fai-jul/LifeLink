package com.lifelink.db;

import com.lifelink.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BloodRequestRepository {

    private final Connection conn = DatabaseManager.getConnection();

    public synchronized BloodRequest create(BloodRequest request) {
        String sql = """
                INSERT INTO blood_requests
                    (recipient_id, blood_type, quantity, location, latitude, longitude, urgency, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.getRecipientId());
            ps.setString(2, request.getBloodType().name());
            ps.setInt(3, request.getQuantity());
            ps.setString(4, request.getLocation());
            ps.setDouble(5, request.getLatitude());
            ps.setDouble(6, request.getLongitude());
            ps.setString(7, request.getUrgency().name());
            ps.setString(8, request.getStatus().name());
            ps.setString(9, request.getCreatedAt().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) request.setId(keys.getInt(1));
            }
            addHistory(request.getId(), request.getStatus(), "Request created");
            return request;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create blood request", e);
        }
    }

    public synchronized List<BloodRequest> findForRecipient(int recipientId) {
        return find("WHERE recipient_id = ? ORDER BY created_at DESC", recipientId);
    }

    public synchronized List<BloodRequest> findActive() {
        return find("WHERE status IN ('PENDING', 'SEARCHING', 'MATCHED') ORDER BY created_at DESC");
    }

    public synchronized void updateStatus(int requestId, RequestStatus status, String note) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE blood_requests SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, requestId);
            ps.executeUpdate();
            addHistory(requestId, status, note);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update blood request", e);
        }
    }

    public synchronized List<RequestStatusEvent> historyFor(int requestId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM request_status_history WHERE request_id = ? ORDER BY timestamp")) {
            ps.setInt(1, requestId);
            List<RequestStatusEvent> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new RequestStatusEvent(
                            requestId,
                            RequestStatus.valueOf(rs.getString("status")),
                            rs.getString("note"),
                            LocalDateTime.parse(rs.getString("timestamp"))));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load request timeline", e);
        }
    }

    private List<BloodRequest> find(String suffix, Object... values) {
        String sql = "SELECT * FROM blood_requests " + suffix;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            List<BloodRequest> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load blood requests", e);
        }
    }

    private void addHistory(int requestId, RequestStatus status, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO request_status_history (request_id, status, note, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, requestId);
            ps.setString(2, status.name());
            ps.setString(3, note);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    private BloodRequest map(ResultSet rs) throws SQLException {
        return new BloodRequest(
                rs.getInt("id"),
                rs.getInt("recipient_id"),
                BloodType.valueOf(rs.getString("blood_type")),
                rs.getInt("quantity"),
                rs.getString("location"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                RequestPriority.valueOf(rs.getString("urgency")),
                RequestStatus.valueOf(rs.getString("status")),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
