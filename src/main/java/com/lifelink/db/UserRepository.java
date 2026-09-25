package com.lifelink.db;

import com.lifelink.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles persistence for all three user roles. Each role's row in
 * `users` is paired with a row in its role-specific table
 * (donors / recipients / blood_banks), inserted together in one
 * transaction so the two tables never drift out of sync.
 */
public class UserRepository {

    private final Connection conn = DatabaseManager.getConnection();

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email == null ? "" : email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up user by email", e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up user by id", e);
        }
        return Optional.empty();
    }

    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }

    /** Inserts the base user row plus its role-specific row, in one transaction. */
    public User save(User user) {
        try {
            conn.setAutoCommit(false);
            int userId = insertBaseUser(user);
            user.setId(userId);

            if (user instanceof Donor donor) {
                insertDonorRow(userId, donor);
            } else if (user instanceof Recipient) {
                insertRecipientRow(userId);
            } else if (user instanceof BloodBank bank) {
                insertBloodBankRow(userId, bank);
            }

            conn.commit();
            return user;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new RuntimeException("Failed to save user", e);
        } finally {
            resetAutoCommit();
        }
    }

    private int insertBaseUser(User user) throws SQLException {
        String sql = """
            INSERT INTO users (name, email, password, phone, role, location, latitude, longitude, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getLocation());
            ps.setDouble(7, user.getLatitude());
            ps.setDouble(8, user.getLongitude());
            ps.setString(9, LocalDateTime.now().toString());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No generated key returned for new user");
    }

    private void insertDonorRow(int userId, Donor donor) throws SQLException {
        String sql = """
            INSERT INTO donors (user_id, blood_type, age, weight, last_donation_date, available)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, donor.getBloodType().name());
            ps.setInt(3, donor.getAge());
            ps.setDouble(4, donor.getWeight());
            ps.setString(5, donor.getLastDonationDate() == null ? null : donor.getLastDonationDate().toString());
            ps.setInt(6, donor.isAvailable() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    private void insertRecipientRow(int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO recipients (user_id) VALUES (?)")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private void insertBloodBankRow(int userId, BloodBank bank) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO blood_banks (user_id, address) VALUES (?, ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, bank.getAddress());
            ps.executeUpdate();
        }
    }

    /** Updates donor-specific fields (availability, last donation date, etc). */
    public void updateDonor(Donor donor) {
        String sql = """
            UPDATE donors SET blood_type = ?, age = ?, weight = ?, last_donation_date = ?, available = ?
            WHERE user_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, donor.getBloodType().name());
            ps.setInt(2, donor.getAge());
            ps.setDouble(3, donor.getWeight());
            ps.setString(4, donor.getLastDonationDate() == null ? null : donor.getLastDonationDate().toString());
            ps.setInt(5, donor.isAvailable() ? 1 : 0);
            ps.setInt(6, donor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update donor", e);
        }
    }

    /** Returns all donors, optionally filtered to only currently-available ones. */
    public List<Donor> findAllDonors(boolean availableOnly) {
        String sql = """
            SELECT u.*, d.blood_type, d.age, d.weight, d.last_donation_date, d.available
            FROM users u JOIN donors d ON u.id = d.user_id
            WHERE (? = 0 OR d.available = 1)
            ORDER BY u.name
        """;
        List<Donor> donors = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, availableOnly ? 1 : 0);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    donors.add(mapDonor(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch donors", e);
        }
        return donors;
    }

    public List<Integer> findAllBloodBankIds() {
        String sql = "SELECT user_id FROM blood_banks ORDER BY user_id";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getInt(1));
            return ids;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch blood banks", e);
        }
    }

    public List<BloodBank> findAllBloodBanks() {
        String sql = "SELECT u.*, b.address FROM users u JOIN blood_banks b ON u.id = b.user_id ORDER BY u.name";
        List<BloodBank> banks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) banks.add(new BloodBank(
                    rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                    rs.getString("password"), rs.getString("phone"), rs.getString("location"),
                    rs.getDouble("latitude"), rs.getDouble("longitude"),
                    LocalDateTime.parse(rs.getString("created_at")), rs.getString("address")));
            return banks;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch blood banks", e);
        }
    }

    public List<Recipient> findAllRecipients() {
        String sql = "SELECT * FROM users WHERE role = 'RECIPIENT' ORDER BY name";
        List<Recipient> recipients = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) recipients.add(mapRecipient(rs));
            return recipients;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch recipients", e);
        }
    }

    // ---- Row mapping helpers ----

    private User mapUser(ResultSet rs) throws SQLException {
        UserRole role = UserRole.valueOf(rs.getString("role"));
        int id = rs.getInt("id");
        return switch (role) {
            case DONOR -> loadDonor(id, rs);
            case RECIPIENT -> mapRecipient(rs);
            case BLOOD_BANK -> loadBloodBank(id, rs);
                case DOCTOR, LAB_TECHNICIAN, ADMINISTRATOR, RECEPTIONIST -> new StaffUser(
                    id, rs.getString("name"), rs.getString("email"), rs.getString("password"),
                    rs.getString("phone"), role, rs.getString("location"),
                    rs.getDouble("latitude"), rs.getDouble("longitude"),
                    LocalDateTime.parse(rs.getString("created_at")));
        };
    }

    private Donor loadDonor(int userId, ResultSet baseRs) throws SQLException {
        String sql = "SELECT * FROM donors WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Donor(
                            userId,
                            baseRs.getString("name"),
                            baseRs.getString("email"),
                            baseRs.getString("password"),
                            baseRs.getString("phone"),
                            baseRs.getString("location"),
                            baseRs.getDouble("latitude"),
                            baseRs.getDouble("longitude"),
                            LocalDateTime.parse(baseRs.getString("created_at")),
                            BloodType.valueOf(rs.getString("blood_type")),
                            rs.getInt("age"),
                            rs.getDouble("weight"),
                            rs.getString("last_donation_date") == null ? null : LocalDate.parse(rs.getString("last_donation_date")),
                            rs.getInt("available") == 1
                    );
                }
            }
        }
        throw new RuntimeException("Donor row missing for user_id=" + userId);
    }

    private Donor mapDonor(ResultSet rs) throws SQLException {
        return new Donor(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("phone"),
                rs.getString("location"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                LocalDateTime.parse(rs.getString("created_at")),
                BloodType.valueOf(rs.getString("blood_type")),
                rs.getInt("age"),
                rs.getDouble("weight"),
                rs.getString("last_donation_date") == null ? null : LocalDate.parse(rs.getString("last_donation_date")),
                rs.getInt("available") == 1
        );
    }

    private Recipient mapRecipient(ResultSet rs) throws SQLException {
        return new Recipient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("phone"),
                rs.getString("location"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }

    private BloodBank loadBloodBank(int userId, ResultSet baseRs) throws SQLException {
        String sql = "SELECT * FROM blood_banks WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BloodBank(
                            userId,
                            baseRs.getString("name"),
                            baseRs.getString("email"),
                            baseRs.getString("password"),
                            baseRs.getString("phone"),
                            baseRs.getString("location"),
                            baseRs.getDouble("latitude"),
                            baseRs.getDouble("longitude"),
                            LocalDateTime.parse(baseRs.getString("created_at")),
                            rs.getString("address")
                    );
                }
            }
        }
        throw new RuntimeException("Blood bank row missing for user_id=" + userId);
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void resetAutoCommit() {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }
}
