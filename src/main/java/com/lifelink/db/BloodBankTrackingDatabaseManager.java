package com.lifelink.db;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class BloodBankTrackingDatabaseManager {

    private static final String DB_FILE = resolveDatabaseFile();
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection;

    private BloodBankTrackingDatabaseManager() {
    }

    private static String resolveDatabaseFile() {
        String configuredPath = System.getProperty("lifelink.bloodbank.db.path");
        if (configuredPath != null && !configuredPath.isBlank()) {
            return configuredPath;
        }

        try {
            Path workingDirectory = Paths.get(System.getProperty("user.dir"));
            return workingDirectory.resolve("bloodbank_tracking.db").toString();
        } catch (Exception ignored) {
            return "bloodbank_tracking.db";
        }
    }

    public static synchronized Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL);
                try (Statement pragma = connection.createStatement()) {
                    // API tracking records live in a separate database from users.
                    pragma.execute("PRAGMA foreign_keys = OFF;");
                }
                initSchema();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to blood bank tracking database", e);
            }
        }
        return connection;
    }

    private static void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS blood_bank_donor_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    blood_bank_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    blood_type TEXT,
                    location TEXT,
                    phone TEXT,
                    last_donation_date TEXT,
                    status TEXT NOT NULL DEFAULT 'AVAILABLE',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (blood_bank_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS blood_bank_patient_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    blood_bank_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    blood_type TEXT,
                    location TEXT,
                    phone TEXT,
                    case_date TEXT,
                    status TEXT NOT NULL DEFAULT 'IN_TREATMENT',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY (blood_bank_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_tracking_donor_bank ON blood_bank_donor_records(blood_bank_id, status)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_tracking_patient_bank ON blood_bank_patient_records(blood_bank_id, status)");
        }
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // ignored on shutdown
            } finally {
                connection = null;
            }
        }
    }
}
