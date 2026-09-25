package com.lifelink.service;

import com.lifelink.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ReportingService {

    public record InventoryTrend(String period, int units) { }

    public record RetentionMetric(String period, int returningDonors) { }

    private final Connection conn = DatabaseManager.getConnection();

    public List<InventoryTrend> monthlyInventoryTrend(int bloodBankId, int months) {
        String sql = "SELECT substr(collection_date, 1, 7) AS period, COALESCE(SUM(quantity), 0) AS units "
                + "FROM blood_units WHERE blood_bank_id = ? GROUP BY period ORDER BY period DESC LIMIT ?";
        List<InventoryTrend> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bloodBankId);
            ps.setInt(2, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new InventoryTrend(rs.getString("period"), rs.getInt("units")));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build inventory trend", e);
        }
    }

    public int wastageUnits(int bloodBankId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM blood_units "
                + "WHERE blood_bank_id = ? AND status = 'EXPIRED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bloodBankId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate inventory wastage", e);
        }
    }

    public List<RetentionMetric> donorRetention(int months) {
        String sql = "SELECT substr(donation_date, 1, 7) AS period, COUNT(DISTINCT donor_id) AS returning_donors "
                + "FROM donations GROUP BY period ORDER BY period DESC LIMIT ?";
        List<RetentionMetric> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new RetentionMetric(rs.getString("period"), rs.getInt("returning_donors")));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate donor retention", e);
        }
    }

    public List<InventoryTrend> demandForecast(int months) {
        String sql = "SELECT substr(created_at, 1, 7) AS period, COALESCE(SUM(quantity), 0) AS units "
                + "FROM blood_requests GROUP BY period ORDER BY period DESC LIMIT ?";
        List<InventoryTrend> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new InventoryTrend(rs.getString("period"), rs.getInt("units")));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build demand forecast", e);
        }
    }
}