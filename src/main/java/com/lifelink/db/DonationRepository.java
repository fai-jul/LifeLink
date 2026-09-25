package com.lifelink.db;

import com.lifelink.model.BloodType;
import com.lifelink.model.Donation;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DonationRepository {

    private final Connection connection = DatabaseManager.getConnection();

    public synchronized List<Donation> findForDonor(int donorId) {
        String sql = "SELECT * FROM donations WHERE donor_id = ? ORDER BY donation_date DESC";
        List<Donation> donations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, donorId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    donations.add(new Donation(
                            result.getInt("id"),
                            result.getInt("donor_id"),
                            LocalDate.parse(result.getString("donation_date")),
                            BloodType.valueOf(result.getString("blood_type")),
                            result.getInt("quantity")));
                }
            }
            return donations;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load donation history", exception);
        }
    }

    public synchronized Donation save(Donation donation) {
        String sql = "INSERT INTO donations (donor_id, donation_date, blood_type, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, donation.getDonorId());
            statement.setString(2, donation.getDonationDate().toString());
            statement.setString(3, donation.getBloodType().name());
            statement.setInt(4, donation.getQuantity());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) donation.setId(keys.getInt(1));
            }
            return donation;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to record donation", exception);
        }
    }
}
