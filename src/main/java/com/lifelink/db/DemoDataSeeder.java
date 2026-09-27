package com.lifelink.db;

import com.lifelink.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Seeds a linked demo workspace on first launch without replacing user data. */
public final class DemoDataSeeder {

    private DemoDataSeeder() {
    }

    public static void seed() {
        Connection connection = DatabaseManager.getConnection();
        try {
            int donor = user(connection, "Amina Rahman", "donor@lifelink.demo", "DONOR", "01700000001");
            int recipient = user(connection, "Tanvir Hasan", "recipient@lifelink.demo", "RECIPIENT", "01700000002");
            int bank = user(connection, "LifeLink Central Blood Bank", "bank@lifelink.demo", "BLOOD_BANK", "01700000003");
            int staff = user(connection, "Dr. Samira Chowdhury", "staff@lifelink.demo", "ADMINISTRATOR", "01700000004");
            execute(connection, "INSERT OR IGNORE INTO donors (user_id,blood_type,age,weight,last_donation_date,available) VALUES (?, 'O_NEG',29,64,?,1)", donor, LocalDate.now().minusDays(75));
            execute(connection, "INSERT OR IGNORE INTO recipients (user_id) VALUES (?)", recipient);
            execute(connection, "INSERT OR IGNORE INTO blood_banks (user_id,address) VALUES (?, 'Dhanmondi, Dhaka')", bank);

            int donation = firstId(connection, "donations", "donor_id", donor);
            if (donation == 0) donation = generated(connection, "INSERT INTO donations (donor_id,donation_date,blood_type,quantity) VALUES (?,?,?,?)", donor, LocalDate.now().minusDays(2), "O_NEG", 1);
            if (count(connection, "blood_units") == 0) {
                execute(connection, "INSERT INTO blood_units (blood_bank_id,blood_type,component,quantity,collection_date,expiry_date,status,storage_location) VALUES (?, 'O_NEG','WHOLE_BLOOD',8,?,?, 'AVAILABLE','Rack A-03')", bank, LocalDate.now().minusDays(10), LocalDate.now().plusDays(32));
                execute(connection, "INSERT INTO blood_units (blood_bank_id,blood_type,component,quantity,collection_date,expiry_date,status,storage_location) VALUES (?, 'A_POS','PLASMA',5,?,?, 'AVAILABLE','Rack B-01')", bank, LocalDate.now().minusDays(25), LocalDate.now().plusDays(7));
                execute(connection, "INSERT INTO blood_units (blood_bank_id,blood_type,component,quantity,collection_date,expiry_date,status,storage_location) VALUES (?, 'B_POS','WHOLE_BLOOD',3,?,?, 'RESERVED','Rack C-02')", bank, LocalDate.now().minusDays(5), LocalDate.now().plusDays(37));
            }
            if (count(connection, "blood_requests") == 0) {
                int request = generated(connection, "INSERT INTO blood_requests (recipient_id,blood_type,quantity,location,latitude,longitude,urgency,status,created_at) VALUES (?, 'O_NEG',2,'Dhaka Medical College',23.78,90.41,'CRITICAL','SEARCHING',?)", recipient, LocalDateTime.now().minusHours(3));
                execute(connection, "INSERT INTO blood_requests (recipient_id,blood_type,quantity,location,latitude,longitude,urgency,status,created_at) VALUES (?, 'A_POS',1,'United Hospital, Dhaka',23.78,90.41,'NORMAL','PENDING',?)", recipient, LocalDateTime.now().minusHours(2));
                execute(connection, "INSERT INTO request_status_history (request_id,status,note,timestamp) VALUES (?, 'PENDING','Demo request created',?)", request, LocalDateTime.now().minusHours(3));
                execute(connection, "INSERT INTO request_status_history (request_id,status,note,timestamp) VALUES (?, 'SEARCHING','Matching compatible donors',?)", request, LocalDateTime.now().minusHours(2));
            }
            if (count(connection, "notifications") == 0) {
                execute(connection, "INSERT INTO notifications (user_id,message,type,read,created_at) VALUES (?, 'A critical O- request is searching for a compatible donor.','EMERGENCY',0,?)", donor, LocalDateTime.now().minusMinutes(20));
                execute(connection, "INSERT INTO notifications (user_id,message,type,read,created_at) VALUES (?, 'Your request is being matched with nearby blood units.','MATCH',0,?)", recipient, LocalDateTime.now().minusMinutes(15));
                execute(connection, "INSERT INTO notifications (user_id,message,type,read,created_at) VALUES (?, 'A- plasma stock expires in 7 days.','EXPIRY',0,?)", bank, LocalDateTime.now().minusMinutes(10));
                execute(connection, "INSERT INTO notifications (user_id,message,type,read,created_at) VALUES (?, 'Demo workspace is ready with sample activity.','SYSTEM',1,?)", staff, LocalDateTime.now().minusDays(1));
            }
            if (count(connection, "activity_logs") == 0) {
                execute(connection, "INSERT INTO activity_logs (user_id,action,description,timestamp) VALUES (?, 'DONATION_RECORDED','Demo O- donation added to donation history.',?)", donor, LocalDateTime.now().minusDays(2));
                execute(connection, "INSERT INTO activity_logs (user_id,action,description,timestamp) VALUES (?, 'REQUEST_CREATED','Demo critical blood request created.',?)", recipient, LocalDateTime.now().minusHours(3));
                execute(connection, "INSERT INTO activity_logs (user_id,action,description,timestamp) VALUES (?, 'INVENTORY_UPDATED','Demo inventory received from central blood bank.',?)", bank, LocalDateTime.now().minusDays(1));
            }
            if (count(connection, "appointments") == 0) execute(connection, "INSERT INTO appointments (donor_id,scheduled_at,status,notes,created_by,created_at) VALUES (?,?,'SCHEDULED','Demo donation appointment',?,?)", donor, LocalDateTime.now().plusDays(4), staff, LocalDateTime.now().minusDays(1));
            if (count(connection, "lab_screenings") == 0) execute(connection, "INSERT INTO lab_screenings (donation_id,tested_by,hiv,hbv,hcv,syphilis,malaria,overall_result,tested_at,notes) VALUES (?,?,'NEGATIVE','NEGATIVE','NEGATIVE','NEGATIVE','NEGATIVE','PASSED',?,'Demo screening result')", donation, staff, LocalDateTime.now().minusDays(1));
            seedTracking(bank);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to seed demo data", exception);
        }
    }

    private static int user(Connection c, String name, String email, String role, String phone) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM users WHERE email=?")) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) { if (r.next()) return r.getInt(1); }
        }
        return generated(c, "INSERT INTO users (name,email,password,phone,role,location,latitude,longitude,created_at) VALUES (?,?,?,? ,?,'Dhaka',23.8,90.4,?)", name, email, PasswordUtil.hash("demo123"), phone, role, LocalDateTime.now());
    }

    private static void seedTracking(int bank) throws SQLException {
        Connection c = BloodBankTrackingDatabaseManager.getConnection();
        if (count(c, "blood_bank_donor_records") > 0) return;
        String now = LocalDateTime.now().toString();
        execute(c, "INSERT INTO blood_bank_donor_records (blood_bank_id,name,blood_type,location,phone,last_donation_date,status,created_at,updated_at) VALUES (?, 'Amina Rahman','O-','Dhaka','01700000001',?,'AVAILABLE',?,?)", bank, LocalDate.now().minusDays(75), now, now);
        execute(c, "INSERT INTO blood_bank_donor_records (blood_bank_id,name,blood_type,location,phone,last_donation_date,status,created_at,updated_at) VALUES (?, 'Nabil Ahmed','A+','Mirpur, Dhaka','01700000005',?,'AVAILABLE',?,?)", bank, LocalDate.now().minusDays(42), now, now);
        execute(c, "INSERT INTO blood_bank_patient_records (blood_bank_id,name,blood_type,location,phone,case_date,status,created_at,updated_at) VALUES (?, 'Tanvir Hasan','O-','Dhaka Medical College','01700000002',?,'IN_TREATMENT',?,?)", bank, LocalDate.now().minusDays(2), now, now);
    }

    private static int count(Connection c, String table) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM " + table); ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; }
    }

    private static int firstId(Connection c, String table, String column, int value) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM " + table + " WHERE " + column + "=? ORDER BY id LIMIT 1")) { p.setInt(1, value); try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; } }
    }

    private static int generated(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) { bind(p, values); p.executeUpdate(); try (ResultSet r = p.getGeneratedKeys()) { if (r.next()) return r.getInt(1); } }
        throw new SQLException("No generated key returned");
    }

    private static void execute(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { bind(p, values); p.executeUpdate(); }
    }

    private static void bind(PreparedStatement p, Object... values) throws SQLException { for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]); }
}