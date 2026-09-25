# LifeLink — Blood Bank & Emergency Donor Navigation System

A JavaFX desktop application connecting blood donors, recipients, and blood
banks: donor/recipient/blood-bank registration, blood-type compatibility
checking, eligibility tracking, and (in later phases) emergency donor
matching, blood-bank inventory management, and a location/map-based nearby
blood-bank finder.

This README documents the completed Phase 1 and Phase 2 build plus the active
Phase 3 location and analytics work. See
`PROJECT_STATUS.md` for the full roadmap.

## Requirements

- JDK 21+ (JDK 26 is supported with the JavaFX 26 runtime used by the map view)
- Maven 3.9+
- Internet access on first build (Maven downloads JavaFX + SQLite JDBC + org.json from Maven Central)

## How to run

```bash
cd LifeLink
mvn clean javafx:run
```

The first run creates `lifelink.db` (SQLite) in the project's working
directory and initializes the full schema automatically — no manual setup
needed.

## What works right now (Phase 1, Phase 2, and Phase 3)

- Editorial splash screen with a restrained blood-mark settle, fade transitions,
  and "press any key" to continue
- Sign up as **Donor**, **Recipient**, or **Blood Bank**, with role-specific
  fields and full input validation
- Secure login (salted SHA-256 password hashing)
- Role-specific dashboards with a shared sidebar/topbar shell
- Donor dashboard: blood type, live availability toggle, and real
  eligibility calculation (90-day donation gap rule) backed by SQLite
- Recipient emergency requests with donor matching and status timelines
- Blood-bank inventory CRUD with search/filter and expiry tracking
- Background expiry monitoring plus low-stock and expiry-warning alerts
- Concurrent compatibility, eligibility, and distance-based donor matching
- Notifications generated for emergency requests, donor matches, inventory, and expiry events
- Full activity log screens for every account role
- Nearby network screen with saved-coordinate markers and an OpenStreetMap browser handoff
- Nominatim location lookup with timeout-safe offline fallback
- Real blood-bank network statistics, donor blood-type chart, and live summary counters
- Confirmation dialogs for request cancellation and other critical actions
- Full SQLite schema for every table described in the spec (users, donors,
  recipients, blood_banks, blood_units, blood_requests, donations,
  notifications, activity_logs) — ready for Phase 2 to build on
- Activity logging on registration, login, and availability changes
- Blood-bank inventory CRUD with search/filter and expiry monitoring
- Emergency requests with status timelines and concurrent donor matching
- Low-stock, expiry, and donor-match notifications
- Full activity log screens for every account role
- Dark / red / white visual theme applied consistently across every screen

## Project structure

```
src/main/java/com/lifelink/
  Main.java              — application entry point
  SceneManager.java       — scene switching + session state
  model/                  — enums + entity classes (User hierarchy, BloodUnit, BloodRequest, ...)
  db/                     — DatabaseManager (schema) + repository classes
  service/                — authentication, matching, inventory, and request services
  controller/             — one controller per FXML screen
src/main/resources/com/lifelink/
  fxml/                   — splash, login, signup, dashboards, and map
  css/app.css             — the single shared stylesheet
```

## Architecture rules followed

- Controllers never contain business logic — that lives in `service/`
- Database access is isolated to `db/` repository classes
- Compatibility logic lives in `CompatibilityService`; eligibility rules in
  `EligibilityService`
- The JavaFX Application Thread is never blocked; concurrent matching and
  background expiry monitoring use `ExecutorService`/`ScheduledExecutorService`
  with `Platform.runLater()` for UI refreshes
