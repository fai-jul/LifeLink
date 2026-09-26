package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodBankTrackingRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.BloodUnitRepository;
import com.lifelink.model.BloodBank;
import com.lifelink.model.BloodType;
import com.lifelink.model.Notification;
import com.lifelink.util.DateUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class BloodBankDashboardController {

    private static final DateTimeFormatter TODAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);

    @FXML private Label welcomeLabel;
    @FXML private Label notificationsEmptyLabel;
    @FXML private VBox notificationsBox;
    @FXML private Label inventoryMetric;
    @FXML private Label requestsMetric;
    @FXML private Label lowStockMetric;
    @FXML private Label trackedDonorMetric;
    @FXML private Label trackedPatientMetric;
    @FXML private Label inventoryEmptyLabel;
    @FXML private PieChart inventoryChart;
    @FXML private Label sidebarAvatarLabel;
    @FXML private Label sidebarNameLabel;
    @FXML private Label topbarAvatarLabel;
    @FXML private Label topbarDateLabel;

    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final BloodUnitRepository bloodUnitRepository = new BloodUnitRepository();
    private final BloodRequestRepository bloodRequestRepository = new BloodRequestRepository();
    private final BloodBankTrackingRepository trackingRepository = new BloodBankTrackingRepository();
    private BloodBank bloodBank;

    @FXML
    public void initialize() {
        bloodBank = (BloodBank) SceneManager.getCurrentUser();
        welcomeLabel.setText("Welcome, " + bloodBank.getName());
        topbarDateLabel.setText(LocalDate.now().format(TODAY_FORMAT));
        String initial = initials(bloodBank.getName());
        sidebarAvatarLabel.setText(initial);
        topbarAvatarLabel.setText(initial);
        sidebarNameLabel.setText(bloodBank.getName());
        loadMetrics();
        loadNotifications();
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "B";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    private void loadMetrics() {
        int total = bloodUnitRepository.totalAvailable(bloodBank.getId());
        int donorsTracked = trackingRepository.countDonorsForBank(bloodBank.getId());
        int patientsTracked = trackingRepository.countPatientsForBank(bloodBank.getId());

        inventoryMetric.setText(total + " units");
        requestsMetric.setText(String.valueOf(bloodRequestRepository.findActive().size()));
        lowStockMetric.setText(String.valueOf(total < 5 ? 1 : 0));
        trackedDonorMetric.setText(String.valueOf(donorsTracked));
        trackedPatientMetric.setText(String.valueOf(patientsTracked));
        inventoryChart.getData().clear();
        for (BloodType type : BloodType.values()) {
            int quantity = bloodUnitRepository.availableForType(bloodBank.getId(), type);
            if (quantity > 0) inventoryChart.getData().add(new PieChart.Data(type.getLabel(), quantity));
        }
        boolean empty = inventoryChart.getData().isEmpty();
        inventoryEmptyLabel.setVisible(empty);
        inventoryEmptyLabel.setManaged(empty);
        inventoryChart.setVisible(!empty);
        inventoryChart.setManaged(!empty);
    }

    private void loadNotifications() {
        List<Notification> notifications = notificationRepository.findForUser(bloodBank.getId());
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
    public void showDashboard() {
        loadMetrics();
        loadNotifications();
    }

    @FXML
    public void showNotifications() {
        SceneManager.switchTo("notifications.fxml", "Notifications");
    }

    @FXML
    public void openInventory() {
        SceneManager.switchTo("inventory.fxml", "Inventory");
    }

    @FXML
    public void openRequests() {
        SceneManager.switchTo("bloodbank_requests.fxml", "Emergency Requests");
    }

    @FXML
    public void showActivity() {
        SceneManager.switchTo("activity_log.fxml", "Activity Log");
    }

    @FXML
    public void showProfile() {
        info("Profile", bloodBank.getName() + "\n" + bloodBank.getEmail() + "\n" + bloodBank.getPhone()
                + "\n" + bloodBank.getAddress() + "\n" + bloodBank.getLocation());
    }

    @FXML
    public void openMap() {
        SceneManager.switchTo("map.fxml", "Nearby Network");
    }

    @FXML
    public void handleLogout() {
        SceneManager.logout();
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
