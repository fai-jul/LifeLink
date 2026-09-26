package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.DonationRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.BloodType;
import com.lifelink.model.Donation;
import com.lifelink.model.Donor;
import com.lifelink.model.Notification;
import com.lifelink.service.EligibilityService;
import com.lifelink.util.DateUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DonorDashboardController {

    private static final DateTimeFormatter TODAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);
    private static final List<String> BANGLADESHI_DONOR_NAMES = List.of(
            "Rahim Uddin", "Karim Ahmed", "Nasima Akter", "Farhan Hossain",
            "Shahana Begum", "Jahid Hasan", "Mitu Rahman", "Sabbir Khan",
            "Taslima Begum", "Mahmudul Islam", "Lamia Sultana", "Nabil Hossain",
            "Rumana Karim", "Anik Chowdhury", "Shamima Akter", "Hasan Ali",
            "Faria Islam", "Mizanur Rahman", "Ayesha Siddiqua", "Ibrahim Khalil"
    );

    @FXML private Label welcomeLabel;
    @FXML private Label bloodTypeLabel;
    @FXML private Label availabilityPill;
    @FXML private ToggleButton availabilityToggle;
    @FXML private Label eligibilityPill;
    @FXML private Label nextEligibleLabel;
    @FXML private Label notificationsEmptyLabel;
    @FXML private VBox notificationsBox;
    @FXML private VBox emergencyRequestsBox;
    @FXML private VBox donorListBox;
    @FXML private VBox previousDonorsBox;
    @FXML private GridPane compatibilityGrid;
    @FXML private PieChart bloodTypeChart;
    @FXML private BarChart<String, Number> donationBarChart;
    @FXML private LineChart<String, Number> growthLineChart;
    @FXML private Label requestSummaryLabel;
    @FXML private Label sidebarAvatarLabel;
    @FXML private Label sidebarNameLabel;
    @FXML private Label topbarAvatarLabel;
    @FXML private Label topbarDateLabel;

    private final EligibilityService eligibilityService = new EligibilityService();
    private final UserRepository userRepository = new UserRepository();
    private final DonationRepository donationRepository = new DonationRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();
    private final BloodRequestRepository bloodRequestRepository = new BloodRequestRepository();

    private Donor donor;

    @FXML
    public void initialize() {
        donor = (Donor) SceneManager.getCurrentUser();
        welcomeLabel.setText("Welcome, " + donor.getName());
        topbarDateLabel.setText(LocalDate.now().format(TODAY_FORMAT));
        String initial = initials(donor.getName());
        sidebarAvatarLabel.setText(initial);
        topbarAvatarLabel.setText(initial);
        sidebarNameLabel.setText(donor.getName());
        refresh();
        loadDashboardInsights();
        loadEmergencyRequests();
        loadNotifications();
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "D";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    private void refresh() {
        bloodTypeLabel.setText(donor.getBloodType().getLabel());

        availabilityToggle.setSelected(donor.isAvailable());
        applyAvailabilityStyle();

        boolean eligible = eligibilityService.isEligible(donor);
        eligibilityPill.setText(eligible ? "ELIGIBLE" : "NOT ELIGIBLE");
        eligibilityPill.getStyleClass().removeAll("status-eligible", "status-not-eligible");
        eligibilityPill.getStyleClass().add(eligible ? "status-eligible" : "status-not-eligible");

        if (eligible) {
            nextEligibleLabel.setText("You can donate now.");
        } else {
            long days = eligibilityService.daysUntilEligible(donor);
            nextEligibleLabel.setText(days + " day" + (days == 1 ? "" : "s") + " until next eligible donation");
        }
    }

    private void applyAvailabilityStyle() {
        availabilityPill.setText(donor.isAvailable() ? "AVAILABLE" : "NOT AVAILABLE");
        availabilityPill.getStyleClass().removeAll("status-available", "status-unavailable");
        availabilityPill.getStyleClass().add(donor.isAvailable() ? "status-available" : "status-unavailable");
    }

    private void loadNotifications() {
        List<Notification> notifications = notificationRepository.findForUser(donor.getId());
        notificationsBox.getChildren().clear();
        notificationsEmptyLabel.setVisible(notifications.isEmpty());
        notificationsEmptyLabel.setManaged(notifications.isEmpty());
        for (Notification n : notifications) {
            notificationsBox.getChildren().add(buildNotificationRow(n));
        }
    }

    private HBox buildNotificationRow(Notification n) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("list-row-card", rowStyleForType(n.getType().name()));
        row.setMaxWidth(Double.MAX_VALUE);

        Label tag = new Label(n.getType().name());
        tag.getStyleClass().addAll("list-row-tag", tagStyleForType(n.getType().name()));

        VBox textBox = new VBox(2);
        Label message = new Label(n.getMessage());
        message.getStyleClass().add("list-row-title");
        message.setWrapText(true);
        Label meta = new Label(DateUtil.format(n.getCreatedAt()));
        meta.getStyleClass().add("list-row-meta");
        textBox.getChildren().addAll(message, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(tag, textBox);
        return row;
    }

    private String rowStyleForType(String type) {
        return switch (type) {
            case "EMERGENCY" -> "list-row-critical";
            case "EXPIRY" -> "list-row-high";
            case "MATCH" -> "list-row-normal";
            default -> "list-row-info";
        };
    }

    private String tagStyleForType(String type) {
        return switch (type) {
            case "EMERGENCY" -> "tag-emergency";
            case "INVENTORY" -> "tag-inventory";
            case "EXPIRY" -> "tag-expiry";
            case "MATCH" -> "tag-match";
            default -> "tag-system";
        };
    }

    @FXML
    public void handleAvailabilityToggle() {
        donor.setAvailable(availabilityToggle.isSelected());
        userRepository.updateDonor(donor);
        activityLogRepository.log(donor.getId(), "AVAILABILITY_CHANGE",
                donor.getName() + " set availability to " + (donor.isAvailable() ? "AVAILABLE" : "NOT AVAILABLE"));
        applyAvailabilityStyle();
    }

    @FXML
    public void showDashboard() {
        refresh();
        loadDashboardInsights();
        loadEmergencyRequests();
        loadNotifications();
    }

    @FXML
    public void becomeDonor() {
        availabilityToggle.setSelected(true);
        handleAvailabilityToggle();
        info("You are visible", "Your donor profile is now marked as available for compatible emergency requests.");
    }

    @FXML
    public void findDonor() {
        openMap();
    }

    private void loadDashboardInsights() {
        List<Donor> allDonors = userRepository.findAllDonors(false);
        loadBloodTypeChart(allDonors);
        loadDonationTrends(allDonors);
        loadDonorLists(allDonors);
        buildCompatibilityGrid();
    }

    private void loadBloodTypeChart(List<Donor> donors) {
        bloodTypeChart.getData().clear();
        int[] typeCounts = new int[BloodType.values().length];

        if (donors.isEmpty()) {
            int[] fallback = {10, 7, 12, 6, 4, 3, 15, 8};
            typeCounts = fallback;
        } else {
            for (Donor donor : donors) {
                if (donor.getBloodType() != null) {
                    typeCounts[donor.getBloodType().ordinal()]++;
                }
            }
        }

        for (int i = 0; i < BloodType.values().length; i++) {
            if (typeCounts[i] > 0) {
                bloodTypeChart.getData().add(new PieChart.Data(BloodType.values()[i].getLabel(), typeCounts[i]));
            }
        }
    }

    private void loadDonationTrends(List<Donor> donors) {
        String[] months = {"Apr", "May", "Jun", "Jul", "Aug", "Sep"};
        int[] monthlyDonations = calculateMonthlyDonationCounts(donors, months.length);

        donationBarChart.getData().clear();
        XYChart.Series<String, Number> donations = new XYChart.Series<>();
        donations.setName("Monthly donations");
        for (int i = 0; i < months.length; i++) {
            donations.getData().add(new XYChart.Data<>(months[i], monthlyDonations[i]));
        }
        donationBarChart.getData().add(donations);

        growthLineChart.getData().clear();
        XYChart.Series<String, Number> growth = new XYChart.Series<>();
        growth.setName("Network growth");
        int runningTotal = 0;
        for (int i = 0; i < months.length; i++) {
            runningTotal += Math.max(4, monthlyDonations[i]);
            growth.getData().add(new XYChart.Data<>(months[i], runningTotal));
        }
        growthLineChart.getData().add(growth);
    }

    private int[] calculateMonthlyDonationCounts(List<Donor> donors, int monthCount) {
        int[] monthlyDonations = new int[monthCount];
        List<Donation> allDonations = new ArrayList<>();

        for (Donor donor : donors) {
            allDonations.addAll(donationRepository.findForDonor(donor.getId()));
        }

        if (allDonations.isEmpty()) {
            int donorBase = donors.isEmpty() ? 18 : Math.max(6, donors.size());
            for (int i = 0; i < monthCount; i++) {
                monthlyDonations[i] = donorBase / 2 + i + 4;
            }
            return monthlyDonations;
        }

        for (Donation donation : allDonations) {
            if (donation.getDonationDate() == null) continue;
            int monthIndex = Math.min(monthCount - 1, Math.max(0, 5 - donation.getDonationDate().getMonth().getValue() % 6));
            monthlyDonations[monthIndex] += donation.getQuantity();
        }
        return monthlyDonations;
    }

    private void loadDonorLists(List<Donor> donors) {
        List<String> names = donors.stream().map(Donor::getName).filter(name -> name != null && !name.isBlank()).toList();
        donorListBox.getChildren().setAll(
                donorRow(getNameAt(names, 0, "Rahim Uddin"), getBloodTypeAt(donors, 0, BloodType.O_POS), formatDistance(2.4), "Available today"),
                donorRow(getNameAt(names, 1, "Karim Ahmed"), getBloodTypeAt(donors, 1, BloodType.A_NEG), formatDistance(4.1), "Available tomorrow"),
                donorRow(getNameAt(names, 2, "Nasima Akter"), getBloodTypeAt(donors, 2, BloodType.B_POS), formatDistance(5.8), "Available today"));

        previousDonorsBox.getChildren().setAll(
                donorRow(getNameAt(names, 3, "Farhan Hossain"), getBloodTypeAt(donors, 3, BloodType.O_NEG), "Last donation: 12 Sep 2026", "8 donations"),
                donorRow(getNameAt(names, 4, "Shahana Begum"), getBloodTypeAt(donors, 4, BloodType.AB_POS), "Last donation: 28 Aug 2026", "5 donations"),
                donorRow(getNameAt(names, 5, "Jahid Hasan"), getBloodTypeAt(donors, 5, BloodType.A_POS), "Last donation: 04 Aug 2026", "11 donations"));
    }

    private String getNameAt(List<String> names, int index, String fallback) {
        if (index < names.size() && !names.get(index).isBlank()) {
            return names.get(index);
        }
        return fallback;
    }

    private String getBloodTypeAt(List<Donor> donors, int index, BloodType fallback) {
        if (index < donors.size() && donors.get(index).getBloodType() != null) {
            return donors.get(index).getBloodType().getLabel();
        }
        return fallback.getLabel();
    }

    private String formatDistance(double kilometers) {
        return String.format(Locale.US, "%.1f km away", kilometers);
    }

    private HBox donorRow(String name, String bloodType, String detail, String status) {
        Label type = new Label(bloodType);
        type.getStyleClass().add("blood-type-chip");
        Label donorName = new Label(name);
        donorName.getStyleClass().add("list-row-title");
        Label donorDetail = new Label(detail);
        donorDetail.getStyleClass().add("list-row-meta");
        VBox copy = new VBox(2, donorName, donorDetail);
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label availability = new Label(status);
        availability.getStyleClass().add("donor-status");
        HBox row = new HBox(10, type, copy, availability);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("donor-row");
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private void buildCompatibilityGrid() {
        compatibilityGrid.getChildren().clear();
        String[] headers = {"Donor", "Can donate to", "Why"};
        for (int column = 0; column < headers.length; column++) {
            Label header = new Label(headers[column]);
            header.getStyleClass().add("grid-header");
            compatibilityGrid.add(header, column, 0);
        }
        String[][] rows = {
                {"O-", "All blood types", "No A, B, or Rh antigens"},
                {"O+", "O+, A+, B+, AB+", "Rh-positive recipients"},
                {"A-", "A-, A+, AB-, AB+", "A antigen and compatible Rh"},
                {"B-", "B-, B+, AB-, AB+", "B antigen and compatible Rh"},
                {"AB+", "AB+", "AB antigens require AB+"}
        };
        for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
            for (int column = 0; column < rows[rowIndex].length; column++) {
                Label cell = new Label(rows[rowIndex][column]);
                cell.getStyleClass().add(column == 0 ? "blood-type-chip" : "grid-cell");
                cell.setWrapText(true);
                compatibilityGrid.add(cell, column, rowIndex + 1);
            }
        }
    }

    @FXML
    public void showNotifications() {
        SceneManager.switchTo("notifications.fxml", "Notifications");
    }

    @FXML
    public void showActivity() {
        SceneManager.switchTo("activity_log.fxml", "Activity Log");
    }

    @FXML
    public void showProfile() {
        info("Profile", donor.getName() + "\n" + donor.getEmail() + "\n" + donor.getPhone() + "\n" + donor.getLocation());
    }

    @FXML
    public void openRequests() {
        SceneManager.switchTo("donor_requests.fxml", "Emergency Requests");
    }

    @FXML
    public void openHistory() {
        SceneManager.switchTo("donation_history.fxml", "Donation History");
    }

    @FXML
    public void openMap() {
        SceneManager.switchTo("map.fxml", "Blood Network");
    }

    @FXML
    public void openStatistics() {
        SceneManager.switchTo("statistics.fxml", "Statistics");
    }

    @FXML
    public void handleLogout() {
        SceneManager.logout();
    }

    private void loadEmergencyRequests() {
        emergencyRequestsBox.getChildren().clear();
        List<BloodRequest> requests = bloodRequestRepository.findActive().stream()
                .filter(request -> donor.getBloodType().canDonateTo(request.getBloodType()))
                .toList();
        requestSummaryLabel.setText(requests.isEmpty()
                ? "No compatible active requests right now."
                : requests.size() + " compatible request" + (requests.size() == 1 ? "" : "s") + " seeking help");
        for (BloodRequest request : requests.stream().limit(4).toList()) {
            emergencyRequestsBox.getChildren().add(buildRequestRow(request));
        }
        if (requests.isEmpty()) {
            Label empty = new Label("No compatible emergency requests right now. Check back soon.");
            empty.getStyleClass().add("empty-state-card");
            empty.setMaxWidth(Double.MAX_VALUE);
            emergencyRequestsBox.getChildren().add(empty);
        }
    }

    private HBox buildRequestRow(BloodRequest request) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("list-row-card", rowStyleForPriority(request.getUrgency().name()));
        row.setMaxWidth(Double.MAX_VALUE);

        Label tag = new Label(request.getUrgency().name());
        tag.getStyleClass().addAll("list-row-tag",
                request.getUrgency().name().equals("CRITICAL") ? "tag-emergency" : "tag-expiry");

        VBox textBox = new VBox(2);
        Label title = new Label(request.getBloodType().getLabel() + "  ·  " + request.getQuantity() + " unit(s) needed");
        title.getStyleClass().add("list-row-title");
        Label meta = new Label(safe(request.getLocation()));
        meta.getStyleClass().add("list-row-meta");
        textBox.getChildren().addAll(title, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(tag, textBox);
        return row;
    }

    private String rowStyleForPriority(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> "list-row-critical";
            case "HIGH" -> "list-row-high";
            default -> "list-row-normal";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Location unavailable" : value;
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
