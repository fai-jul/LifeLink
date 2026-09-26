package com.lifelink.db;

import com.lifelink.model.BloodBankDonorRecord;
import com.lifelink.model.BloodBankPatientRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BloodBankTrackingRepository {

    private final Connection connection = BloodBankTrackingDatabaseManager.getConnection();

    public void saveDonor(BloodBankDonorRecord donor) {
        LocalDateTime now = LocalDateTime.now();
        donor.setUpdatedAt(now);
        if (donor.getCreatedAt() == null) {
            donor.setCreatedAt(now);
        }

        String sql = donor.getId() > 0
                ? "UPDATE blood_bank_donor_records SET blood_bank_id = ?, name = ?, blood_type = ?, location = ?, phone = ?, last_donation_date = ?, status = ?, updated_at = ? WHERE id = ?"
                : "INSERT INTO blood_bank_donor_records (blood_bank_id, name, blood_type, location, phone, last_donation_date, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, donor.getBloodBankId());
            statement.setString(2, donor.getName());
            statement.setString(3, donor.getBloodType());
            statement.setString(4, donor.getLocation());
            statement.setString(5, donor.getPhone());
            statement.setString(6, donor.getLastDonationDate() == null ? null : donor.getLastDonationDate().toString());
            statement.setString(7, donor.getStatus() == null ? "AVAILABLE" : donor.getStatus());
            if (donor.getId() > 0) {
                statement.setString(8, donor.getUpdatedAt().toString());
                statement.setInt(9, donor.getId());
            } else {
                statement.setString(8, donor.getCreatedAt().toString());
                statement.setString(9, donor.getUpdatedAt().toString());
            }
            statement.executeUpdate();

            if (donor.getId() <= 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        donor.setId(keys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save blood bank donor record", e);
        }
    }

    public List<BloodBankDonorRecord> findDonorsForBank(int bloodBankId) {
        String sql = "SELECT * FROM blood_bank_donor_records WHERE blood_bank_id = ? ORDER BY updated_at DESC";
        List<BloodBankDonorRecord> records = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bloodBankId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapDonor(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load donor records", e);
        }
        return records;
    }

    public int countDonorsForBank(int bloodBankId) {
        String sql = "SELECT COUNT(*) FROM blood_bank_donor_records WHERE blood_bank_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bloodBankId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count donor records", e);
        }
        return 0;
    }

    public void savePatient(BloodBankPatientRecord patient) {
        LocalDateTime now = LocalDateTime.now();
        patient.setUpdatedAt(now);
        if (patient.getCreatedAt() == null) {
            patient.setCreatedAt(now);
        }

        String sql = patient.getId() > 0
                ? "UPDATE blood_bank_patient_records SET blood_bank_id = ?, name = ?, blood_type = ?, location = ?, phone = ?, case_date = ?, status = ?, updated_at = ? WHERE id = ?"
                : "INSERT INTO blood_bank_patient_records (blood_bank_id, name, blood_type, location, phone, case_date, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, patient.getBloodBankId());
            statement.setString(2, patient.getName());
            statement.setString(3, patient.getBloodType());
            statement.setString(4, patient.getLocation());
            statement.setString(5, patient.getPhone());
            statement.setString(6, patient.getCaseDate() == null ? null : patient.getCaseDate().toString());
            statement.setString(7, patient.getStatus() == null ? "IN_TREATMENT" : patient.getStatus());
            if (patient.getId() > 0) {
                statement.setString(8, patient.getUpdatedAt().toString());
                statement.setInt(9, patient.getId());
            } else {
                statement.setString(8, patient.getCreatedAt().toString());
                statement.setString(9, patient.getUpdatedAt().toString());
            }
            statement.executeUpdate();

            if (patient.getId() <= 0) {
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        patient.setId(keys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save blood bank patient record", e);
        }
    }

    public List<BloodBankPatientRecord> findPatientsForBank(int bloodBankId) {
        String sql = "SELECT * FROM blood_bank_patient_records WHERE blood_bank_id = ? ORDER BY updated_at DESC";
        List<BloodBankPatientRecord> records = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bloodBankId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapPatient(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load patient records", e);
        }
        return records;
    }

    public int countPatientsForBank(int bloodBankId) {
        String sql = "SELECT COUNT(*) FROM blood_bank_patient_records WHERE blood_bank_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bloodBankId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count patient records", e);
        }
        return 0;
    }

    private BloodBankDonorRecord mapDonor(ResultSet resultSet) throws SQLException {
        BloodBankDonorRecord donor = new BloodBankDonorRecord();
        donor.setId(resultSet.getInt("id"));
        donor.setBloodBankId(resultSet.getInt("blood_bank_id"));
        donor.setName(resultSet.getString("name"));
        donor.setBloodType(resultSet.getString("blood_type"));
        donor.setLocation(resultSet.getString("location"));
        donor.setPhone(resultSet.getString("phone"));
        String lastDonationDate = resultSet.getString("last_donation_date");
        donor.setLastDonationDate(lastDonationDate == null || lastDonationDate.isBlank() ? null : LocalDate.parse(lastDonationDate));
        donor.setStatus(resultSet.getString("status"));
        donor.setCreatedAt(LocalDateTime.parse(resultSet.getString("created_at")));
        donor.setUpdatedAt(LocalDateTime.parse(resultSet.getString("updated_at")));
        return donor;
    }

    private BloodBankPatientRecord mapPatient(ResultSet resultSet) throws SQLException {
        BloodBankPatientRecord patient = new BloodBankPatientRecord();
        patient.setId(resultSet.getInt("id"));
        patient.setBloodBankId(resultSet.getInt("blood_bank_id"));
        patient.setName(resultSet.getString("name"));
        patient.setBloodType(resultSet.getString("blood_type"));
        patient.setLocation(resultSet.getString("location"));
        patient.setPhone(resultSet.getString("phone"));
        String caseDate = resultSet.getString("case_date");
        patient.setCaseDate(caseDate == null || caseDate.isBlank() ? null : LocalDate.parse(caseDate));
        patient.setStatus(resultSet.getString("status"));
        patient.setCreatedAt(LocalDateTime.parse(resultSet.getString("created_at")));
        patient.setUpdatedAt(LocalDateTime.parse(resultSet.getString("updated_at")));
        return patient;
    }
}
