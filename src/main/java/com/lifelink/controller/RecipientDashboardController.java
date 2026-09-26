package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Notification;
import com.lifelink.model.Recipient;
import com.lifelink.util.DateUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class RecipientDashboardController {

    private static final DateTimeFormatter TODAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH);

    @FXML private Label welcomeLabel;
    @FXML private Label notificationsEmptyLabel;
    @FXML private VBox notificationsBox;
    @FXML private VBox activeRequestsBox;
    @FXML private Label requestSummaryLabel;
    @FXML private Label sidebarAvatarLabel;
    @FXML private Label sidebarNameLabel;
    @FXML private Label topbarAvatarLabel;
    @FXML private Label topbarDateLabel;

    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final BloodRequestRepository bloodRequestRepository = new BloodRequestRepository();
    private Recipient recipient;

    @FXML
    public void initialize() {
        recipient = (Recipient) SceneManager.getCurrentUser();
        welcomeLabel.setText("Welcome, " + recipient.getName());
        topbarDateLabel.setText(LocalDate.now().format(TODAY_FORMAT));
        String initial = initials(recipient.getName());
        sidebarAvatarLabel.setText(initial);
        topbarAvatarLabel.setText(initial);
        sidebarNameLabel.setText(recipient.getName());
        loadRequests();
        loadNotifications();
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "R";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase();
    }

    private void loadNotifications() {
        List<Notification> notifications = notificationRepository.findForUser(recipient.getId());
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
        loadRequests();
        loadNotifications();
    }

    @FXML
    public void showNotifications() {
        SceneManager.switchTo("notifications.fxml", "Notifications");
    }

    @FXML
    public void showProfile() {
        info("Profile", recipient.getName() + "\n" + recipient.getEmail() + "\n" + recipient.getPhone() + "\n" + recipient.getLocation());
    }

    @FXML
    public void openRequests() {
        SceneManager.switchTo("recipient_requests.fxml", "Emergency Requests");
    }

    @FXML
    public void openMap() {
        SceneManager.switchTo("map.fxml", "Blood Network");
    }

    @FXML
    public void showActivity() {
        SceneManager.switchTo("activity_log.fxml", "Activity Log");
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

    private void loadRequests() {
        activeRequestsBox.getChildren().clear();
        List<BloodRequest> requests = bloodRequestRepository.findForRecipient(recipient.getId());
        long activeCount = requests.stream().filter(request -> request.getStatus() == com.lifelink.model.RequestStatus.PENDING
            || request.getStatus() == com.lifelink.model.RequestStatus.SEARCHING
            || request.getStatus() == com.lifelink.model.RequestStatus.MATCHED).count();
        requestSummaryLabel.setText(activeCount + " active request" + (activeCount == 1 ? "" : "s") + "  /  " + requests.size() + " total");
        for (BloodRequest request : requests.stream().limit(4).toList()) {
            activeRequestsBox.getChildren().add(buildRequestRow(request));
        }
        if (requests.isEmpty()) {
            Label empty = new Label("No requests yet. Create one to start donor matching.");
            empty.getStyleClass().add("empty-state-card");
            empty.setMaxWidth(Double.MAX_VALUE);
            activeRequestsBox.getChildren().add(empty);
        }
    }

    private HBox buildRequestRow(BloodRequest request) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("list-row-card", rowStyleForStatus(request.getStatus().name()));
        row.setMaxWidth(Double.MAX_VALUE);

        Label tag = new Label(request.getStatus().name());
        tag.getStyleClass().addAll("list-row-tag", tagStyleForStatus(request.getStatus().name()));

        VBox textBox = new VBox(2);
        Label title = new Label("#" + request.getId() + "  " + request.getBloodType().getLabel()
                + "  ·  " + request.getQuantity() + " unit(s)");
        title.getStyleClass().add("list-row-title");
        Label meta = new Label(request.getLocation() == null ? "Location unavailable" : request.getLocation());
        meta.getStyleClass().add("list-row-meta");
        textBox.getChildren().addAll(title, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(tag, textBox);
        return row;
    }

    private String rowStyleForStatus(String status) {
        return switch (status) {
            case "PENDING", "SEARCHING" -> "list-row-high";
            case "MATCHED", "FULFILLED" -> "list-row-normal";
            case "CANCELLED", "NO_MATCH" -> "list-row-info";
            default -> "list-row-info";
        };
    }

    private String tagStyleForStatus(String status) {
        return switch (status) {
            case "PENDING", "SEARCHING" -> "tag-expiry";
            case "MATCHED", "FULFILLED" -> "tag-match";
            default -> "tag-system";
        };
    }
}
