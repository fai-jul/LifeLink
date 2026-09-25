# LifeLink — Build Status & Roadmap

## Phase 1 — COMPLETE (this delivery)
Tier 1 foundation, per the project spec's own priority order:

- [x] Project setup (Maven, JavaFX, SQLite JDBC, org.json)
- [x] Full SQLite schema (all 9 tables from the spec)
- [x] Model layer: User hierarchy, all enums, all entity classes
- [x] AuthenticationService (register/login, salted password hashing, validation)
- [x] CompatibilityService (BloodType.canDonateTo() + lookup helper)
- [x] EligibilityService (90-day donation gap rule, next-eligible-date calc)
- [x] Splash screen with animation sequence
- [x] Login / Signup (role-aware form: Donor / Recipient / Blood Bank)
- [x] Role-specific dashboards (sidebar + topbar shell, dark/red/white theme)
- [x] Donor: live availability toggle + real eligibility status, persisted to SQLite
- [x] Activity logging (registration, login, availability changes)
- [x] Notification read model wired into all three dashboards (empty-state ready)

## Phase 2 — COMPLETE
Tier 1 (remaining) + Tier 2, per the spec:

- [x] Blood bank inventory CRUD (add/edit/remove/search/filter blood units)
- [x] Background expiry monitor (ScheduledExecutorService + Platform.runLater())
- [x] Low-stock and expiry-warning alerts
- [x] Emergency blood request flow (create → status timeline)
- [x] MatchingService: compatibility + eligibility + distance scoring
- [x] Concurrent donor search/notification simulation (ExecutorService)
- [x] Notification generation wired to matching/inventory events
- [x] Full activity log screen (not just the dashboard preview)

## Phase 3 — Location, Map & Analytics (in progress)
Tier 2 (remaining) + Tier 3:

- [x] LocationService: Nominatim/OpenStreetMap API call + JSON parsing
- [x] Haversine distance calculation + nearest-first marker data
- [x] API failure / offline fallback handling
- [x] **Nearby network map handoff (OpenStreetMap browser + in-app markers)**
      showing only persisted donor, bank, and blood-request markers with valid coordinates
- [x] Dashboard PieChart (inventory by blood type) + summary counters
- [x] Confirmation dialogs for destructive/critical actions
- [x] Polished empty states across every remaining screen

## Phase 4 — Polish & Stretch (next)

- [ ] Donation history tracker
- [ ] PriorityQueue for simultaneous urgent requests
- [ ] CSV/JSON export
- [ ] Cached location results for offline resilience
- [ ] Additional charts (requests by status, donations over time)

## Operational Capability Audit — 2026-09-21

- [x] Donor registration, eligibility screening, donation history, and appointment persistence
- [x] Component-aware inventory for whole blood, PRBC, FFP, platelets, and cryoprecipitate, including storage location
- [x] Component separation persistence
- [x] Lab marker screening persistence for HIV, HBV, HCV, syphilis, and malaria
- [x] Cross-match test persistence with ABO compatibility result
- [x] Recipient/hospital request flow, emergency matching, and status history
- [x] Transfer and dispatch persistence with cold-chain temperature fields
- [x] Staff roles and permission enforcement service for doctors, lab technicians, administrators, and receptionists
- [x] Immutable activity-log database triggers and adverse-reaction reporting persistence
- [x] Inventory, wastage, donor-retention, and demand-report query service
- [x] Durable queued handoff for SMS/email notification providers
- [ ] External SMS/email delivery provider configuration and credentials
- [ ] Full staff forms and dedicated screens for every operational workflow

---

**Note on the map feature:** the in-app view is a lightweight JavaFX map
surface that plots persisted latitude/longitude values without requiring a
paid SDK or remote tile service. Records without coordinates remain visible in
the location list and are clearly marked as not plotted; the OpenStreetMap
button provides the external map handoff.
