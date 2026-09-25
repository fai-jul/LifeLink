package com.lifelink.db;

import com.lifelink.model.AppointmentStatus;
import com.lifelink.model.BloodComponent;
import com.lifelink.model.LabResult;
import com.lifelink.model.TransferStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Persistence boundary for operational workflows beyond the original MVP. */
public class OperationalRepository {

    private final Connection conn = DatabaseManager.getConnection();

    public int scheduleAppointment(int donorId, LocalDateTime scheduledAt, AppointmentStatus status,
                                   String notes, int createdBy) {
        String sql = "INSERT INTO appointments (donor_id, scheduled_at, status, notes, created_by, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, donorId);
            ps.setString(2, scheduledAt.toString());
            ps.setString(3, status.name());
            ps.setString(4, notes);
            ps.setInt(5, createdBy);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw failure("schedule appointment", e);
        }
    }

    public int recordComponentSeparation(Integer donationId, int sourceUnitId, BloodComponent component,
                                         int quantity, LocalDate expiryDate, int recordedBy) {
        String sql = "INSERT INTO component_separations "
                + "(donation_id, source_unit_id, component, quantity, expiry_date, recorded_by, recorded_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (donationId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, donationId);
            ps.setInt(2, sourceUnitId);
            ps.setString(3, component.name());
            ps.setInt(4, quantity);
            ps.setString(5, expiryDate.toString());
            ps.setInt(6, recordedBy);
            ps.setString(7, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw failure("record component separation", e);
        }
    }

    public int recordLabScreening(int donationId, int testedBy, LabResult hiv, LabResult hbv,
                                  LabResult hcv, LabResult syphilis, LabResult malaria,
                                  LabResult overall, String notes) {
        String sql = "INSERT INTO lab_screenings "
                + "(donation_id, tested_by, hiv, hbv, hcv, syphilis, malaria, overall_result, tested_at, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, donationId);
            ps.setInt(2, testedBy);
            ps.setString(3, hiv.name());
            ps.setString(4, hbv.name());
            ps.setString(5, hcv.name());
            ps.setString(6, syphilis.name());
            ps.setString(7, malaria.name());
            ps.setString(8, overall.name());
            ps.setString(9, LocalDateTime.now().toString());
            ps.setString(10, notes);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw failure("record lab screening", e);
        }
    }

    public int recordCrossMatch(int requestId, Integer bloodUnitId, String recipientBloodType,
                                String donorBloodType, boolean compatible, int testedBy, String notes) {
        String sql = "INSERT INTO crossmatch_tests "
                + "(request_id, blood_unit_id, recipient_blood_type, donor_blood_type, compatible, tested_by, tested_at, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requestId);
            if (bloodUnitId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, bloodUnitId);
            ps.setString(3, recipientBloodType);
            ps.setString(4, donorBloodType);
            ps.setInt(5, compatible ? 1 : 0);
            ps.setInt(6, testedBy);
            ps.setString(7, LocalDateTime.now().toString());
            ps.setString(8, notes);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw failure("record cross-match", e);
        }
    }

    public int recordTransfer(int fromBankId, int toBankId, int bloodUnitId, TransferStatus status,
                              LocalDateTime dispatchedAt, Double temperatureMin, Double temperatureMax,
                              int createdBy, String notes) {
        String sql = "INSERT INTO blood_transfers "
                + "(from_bank_id, to_bank_id, blood_unit_id, dispatched_at, temperature_min, temperature_max, status, notes, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fromBankId);
            ps.setInt(2, toBankId);
            ps.setInt(3, bloodUnitId);
            if (dispatchedAt == null) ps.setNull(4, java.sql.Types.VARCHAR); else ps.setString(4, dispatchedAt.toString());
            if (temperatureMin == null) ps.setNull(5, java.sql.Types.REAL); else ps.setDouble(5, temperatureMin);
            if (temperatureMax == null) ps.setNull(6, java.sql.Types.REAL); else ps.setDouble(6, temperatureMax);
            ps.setString(7, status.name());
            ps.setString(8, notes);
            ps.setInt(9, createdBy);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw failure("record blood transfer", e);
        }
    }

    public void recordAdverseReaction(Integer requestId, Integer recipientId, String severity,
                                      String description, int reportedBy) {
        String sql = "INSERT INTO adverse_reactions "
                + "(request_id, recipient_id, severity, description, reported_by, reported_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (requestId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, requestId);
            if (recipientId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, recipientId);
            ps.setString(3, severity);
            ps.setString(4, description);
            ps.setInt(5, reportedBy);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failure("record adverse reaction", e);
        }
    }

    private RuntimeException failure(String operation, SQLException cause) {
        return new RuntimeException("Failed to " + operation, cause);
    }
}