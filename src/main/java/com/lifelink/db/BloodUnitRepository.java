package com.lifelink.db;

import com.lifelink.model.BloodType;
import com.lifelink.model.BloodComponent;
import com.lifelink.model.BloodUnit;
import com.lifelink.model.BloodUnitStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BloodUnitRepository {

    private final Connection conn = DatabaseManager.getConnection();

    public synchronized BloodUnit save(BloodUnit unit) {
        String sql = """
                INSERT INTO blood_units
                    (blood_bank_id, blood_type, component, quantity, collection_date, expiry_date, status, storage_location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, unit.getBloodBankId());
            ps.setString(2, unit.getBloodType().name());
            ps.setString(3, unit.getComponent().name());
            ps.setInt(4, unit.getQuantity());
            ps.setString(5, unit.getCollectionDate().toString());
            ps.setString(6, unit.getExpiryDate().toString());
            ps.setString(7, unit.getStatus().name());
            ps.setString(8, unit.getStorageLocation());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) unit.setId(keys.getInt(1));
            }
            return unit;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add blood unit", e);
        }
    }

    public synchronized void update(BloodUnit unit) {
        String sql = """
                UPDATE blood_units
                SET blood_type = ?, component = ?, quantity = ?, collection_date = ?, expiry_date = ?, status = ?, storage_location = ?
                WHERE id = ? AND blood_bank_id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, unit.getBloodType().name());
            ps.setString(2, unit.getComponent().name());
            ps.setInt(3, unit.getQuantity());
            ps.setString(4, unit.getCollectionDate().toString());
            ps.setString(5, unit.getExpiryDate().toString());
            ps.setString(6, unit.getStatus().name());
            ps.setString(7, unit.getStorageLocation());
            ps.setInt(8, unit.getId());
            ps.setInt(9, unit.getBloodBankId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update blood unit", e);
        }
    }

    public synchronized void delete(int id, int bloodBankId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM blood_units WHERE id = ? AND blood_bank_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, bloodBankId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove blood unit", e);
        }
    }

    public synchronized Optional<BloodUnit> findById(int id, int bloodBankId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM blood_units WHERE id = ? AND blood_bank_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, bloodBankId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find blood unit", e);
        }
    }

    public synchronized List<BloodUnit> findForBank(int bloodBankId, String search,
                                                     BloodType type, BloodUnitStatus status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM blood_units WHERE blood_bank_id = ?");
        List<Object> values = new ArrayList<>();
        values.add(bloodBankId);
        if (search != null && !search.isBlank()) {
            sql.append(" AND (replace(replace(blood_type, '_POS', '+'), '_NEG', '-') LIKE ? OR status LIKE ?)");
            String pattern = "%" + search.trim().toUpperCase() + "%";
            values.add(pattern);
            values.add(pattern);
        }
        if (type != null) {
            sql.append(" AND blood_type = ?");
            values.add(type.name());
        }
        if (status != null) {
            sql.append(" AND status = ?");
            values.add(status.name());
        }
        sql.append(" ORDER BY expiry_date, blood_type");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            List<BloodUnit> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search blood inventory", e);
        }
    }

    public synchronized int totalAvailable(int bloodBankId) {
        return scalarCount("SELECT COALESCE(SUM(quantity), 0) FROM blood_units " +
                "WHERE blood_bank_id = ? AND status = 'AVAILABLE'", bloodBankId);
    }

    public synchronized int availableForType(int bloodBankId, BloodType type) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(quantity), 0) FROM blood_units " +
                        "WHERE blood_bank_id = ? AND blood_type = ? AND status = 'AVAILABLE'")) {
            ps.setInt(1, bloodBankId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count blood type inventory", e);
        }
    }

    public synchronized List<BloodUnit> findExpiredCandidates() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM blood_units WHERE status = 'AVAILABLE' AND expiry_date < ?")) {
            ps.setString(1, LocalDate.now().toString());
            List<BloodUnit> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find expired blood units", e);
        }
    }

    public synchronized List<BloodUnit> findExpiringSoon(int withinDays) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM blood_units WHERE status = 'AVAILABLE' " +
                        "AND expiry_date >= ? AND expiry_date <= ?")) {
            LocalDate today = LocalDate.now();
            ps.setString(1, today.toString());
            ps.setString(2, today.plusDays(withinDays).toString());
            List<BloodUnit> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find expiring blood units", e);
        }
    }

    public synchronized void markExpired(int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE blood_units SET status = 'EXPIRED' WHERE id = ? AND status = 'AVAILABLE'")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark blood unit expired", e);
        }
    }

    private int scalarCount(String sql, int bankId) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bankId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count inventory", e);
        }
    }

    private BloodUnit map(ResultSet rs) throws SQLException {
        return new BloodUnit(
                rs.getInt("id"),
                rs.getInt("blood_bank_id"),
                BloodType.valueOf(rs.getString("blood_type")),
                BloodComponent.valueOf(rs.getString("component")),
                rs.getInt("quantity"),
                LocalDate.parse(rs.getString("collection_date")),
                LocalDate.parse(rs.getString("expiry_date")),
                BloodUnitStatus.valueOf(rs.getString("status")),
                rs.getString("storage_location")
        );
    }
}
