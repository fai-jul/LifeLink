package com.lifelink.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Owns the single SQLite connection used by the application and creates
 * the schema on first run. All repository classes go through here.
 *
 * SQLite handles one writer at a time; for this project's scale a single
 * shared connection with synchronized access is simpler and safer than
 * a pool, and keeps foreign keys enabled consistently.
 */
public final class DatabaseManager {

    private static final String DB_FILE = resolveDatabaseFile();
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection;

    private DatabaseManager() {
    }

    private static String resolveDatabaseFile() {
        String configuredPath = System.getProperty("lifelink.db.path");
        if (configuredPath != null && !configuredPath.isBlank()) {
            return configuredPath;
        }

        try {
            Path codeLocation = Paths.get(DatabaseManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path projectDirectory = codeLocation.toFile().isDirectory()
                    ? codeLocation.getParent().getParent()
                    : codeLocation.getParent();
            return projectDirectory.resolve("lifelink.db").toString();
        } catch (Exception ignored) {
            return Paths.get(System.getProperty("user.dir"), "lifelink.db").toString();
        }
    }

    public static synchronized Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL);
                try (Statement pragma = connection.createStatement()) {
                    pragma.execute("PRAGMA foreign_keys = ON;");
                }
                initSchema();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to SQLite database", e);
            }
        }
        return connection;
    }

    private static void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    phone TEXT,
                    role TEXT NOT NULL,
                    location TEXT,
                    latitude REAL DEFAULT 0,
                    longitude REAL DEFAULT 0,
                    created_at TEXT NOT NULL
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS donors (
                    user_id INTEGER PRIMARY KEY,
                    blood_type TEXT NOT NULL,
                    age INTEGER,
                    weight REAL,
                    last_donation_date TEXT,
                    available INTEGER DEFAULT 1,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS recipients (
                    user_id INTEGER PRIMARY KEY,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS blood_banks (
                    user_id INTEGER PRIMARY KEY,
                    address TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS blood_units (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    blood_bank_id INTEGER NOT NULL,
                    blood_type TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    collection_date TEXT NOT NULL,
                    expiry_date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(user_id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS blood_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recipient_id INTEGER NOT NULL,
                    blood_type TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    location TEXT,
                    latitude REAL DEFAULT 0,
                    longitude REAL DEFAULT 0,
                    urgency TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (recipient_id) REFERENCES recipients(user_id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS donations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    donor_id INTEGER NOT NULL,
                    donation_date TEXT NOT NULL,
                    blood_type TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    FOREIGN KEY (donor_id) REFERENCES donors(user_id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    message TEXT NOT NULL,
                    type TEXT NOT NULL,
                    read INTEGER DEFAULT 0,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    description TEXT,
                    timestamp TEXT NOT NULL
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS request_status_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    request_id INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    note TEXT,
                    timestamp TEXT NOT NULL,
                    FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE
                );
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_blood_units_bank_status ON blood_units(blood_bank_id, status)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_blood_units_expiry ON blood_units(expiry_date)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_requests_recipient ON blood_requests(recipient_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_request_history_request ON request_status_history(request_id)");

            if (!columnExists(st, "blood_units", "component")) {
                st.execute("ALTER TABLE blood_units ADD COLUMN component TEXT NOT NULL DEFAULT 'WHOLE_BLOOD'");
            }
            if (!columnExists(st, "blood_units", "storage_location")) {
                st.execute("ALTER TABLE blood_units ADD COLUMN storage_location TEXT DEFAULT 'Unassigned'");
            }
        }

        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS appointments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    donor_id INTEGER NOT NULL,
                    scheduled_at TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'SCHEDULED',
                    notes TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (donor_id) REFERENCES donors(user_id),
                    FOREIGN KEY (created_by) REFERENCES users(id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS component_separations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    donation_id INTEGER,
                    source_unit_id INTEGER NOT NULL,
                    component TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    expiry_date TEXT NOT NULL,
                    recorded_by INTEGER NOT NULL,
                    recorded_at TEXT NOT NULL,
                    FOREIGN KEY (donation_id) REFERENCES donations(id),
                    FOREIGN KEY (source_unit_id) REFERENCES blood_units(id),
                    FOREIGN KEY (recorded_by) REFERENCES users(id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS lab_screenings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    donation_id INTEGER NOT NULL,
                    tested_by INTEGER NOT NULL,
                    hiv TEXT NOT NULL DEFAULT 'PENDING',
                    hbv TEXT NOT NULL DEFAULT 'PENDING',
                    hcv TEXT NOT NULL DEFAULT 'PENDING',
                    syphilis TEXT NOT NULL DEFAULT 'PENDING',
                    malaria TEXT NOT NULL DEFAULT 'PENDING',
                    overall_result TEXT NOT NULL DEFAULT 'PENDING',
                    tested_at TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY (donation_id) REFERENCES donations(id),
                    FOREIGN KEY (tested_by) REFERENCES users(id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS crossmatch_tests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    request_id INTEGER NOT NULL,
                    blood_unit_id INTEGER,
                    recipient_blood_type TEXT NOT NULL,
                    donor_blood_type TEXT NOT NULL,
                    compatible INTEGER NOT NULL,
                    tested_by INTEGER NOT NULL,
                    tested_at TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY (request_id) REFERENCES blood_requests(id),
                    FOREIGN KEY (blood_unit_id) REFERENCES blood_units(id),
                    FOREIGN KEY (tested_by) REFERENCES users(id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS blood_transfers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_bank_id INTEGER NOT NULL,
                    to_bank_id INTEGER NOT NULL,
                    blood_unit_id INTEGER NOT NULL,
                    dispatched_at TEXT,
                    received_at TEXT,
                    temperature_min REAL,
                    temperature_max REAL,
                    status TEXT NOT NULL DEFAULT 'PLANNED',
                    notes TEXT,
                    created_by INTEGER NOT NULL,
                    FOREIGN KEY (from_bank_id) REFERENCES blood_banks(user_id),
                    FOREIGN KEY (to_bank_id) REFERENCES blood_banks(user_id),
                    FOREIGN KEY (blood_unit_id) REFERENCES blood_units(id),
                    FOREIGN KEY (created_by) REFERENCES users(id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS adverse_reactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    request_id INTEGER,
                    recipient_id INTEGER,
                    severity TEXT NOT NULL,
                    description TEXT NOT NULL,
                    reported_by INTEGER NOT NULL,
                    reported_at TEXT NOT NULL,
                    FOREIGN KEY (request_id) REFERENCES blood_requests(id),
                    FOREIGN KEY (recipient_id) REFERENCES recipients(user_id),
                    FOREIGN KEY (reported_by) REFERENCES users(id)
                )
            """);
            st.execute("""
                CREATE TRIGGER IF NOT EXISTS prevent_activity_log_update
                BEFORE UPDATE ON activity_logs
                BEGIN SELECT RAISE(ABORT, 'Activity logs are immutable'); END
            """);
            st.execute("""
                CREATE TRIGGER IF NOT EXISTS prevent_activity_log_delete
                BEFORE DELETE ON activity_logs
                BEGIN SELECT RAISE(ABORT, 'Activity logs are immutable'); END
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_appointments_donor_status ON appointments(donor_id, status)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_lab_screenings_donation ON lab_screenings(donation_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_transfers_status ON blood_transfers(status)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_reactions_reported_at ON adverse_reactions(reported_at)");
            st.execute("""
                CREATE TABLE IF NOT EXISTS outbound_notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    channel TEXT NOT NULL,
                    destination TEXT NOT NULL,
                    subject TEXT,
                    message TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'QUEUED',
                    created_at TEXT NOT NULL,
                    sent_at TEXT,
                    failure_reason TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_outbound_status ON outbound_notifications(status, created_at)");
        }
    }

    private static boolean columnExists(Statement statement, String table, String column) throws SQLException {
        try (ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
            return false;
        }
    }

    public static synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // ignore on shutdown
            } finally {
                connection = null;
            }
        }
    }
}
