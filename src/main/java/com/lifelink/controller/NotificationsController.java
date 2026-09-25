package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.NotificationRepository;
import com.lifelink.model.Notification;
import com.lifelink.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class NotificationsController {
    @FXML private VBox notificationList;
    @FXML private Label notificationSummary;

    private final NotificationRepository notificationRepository = new NotificationRepository();

    @FXML
    public void initialize() {
        User user = SceneManager.getCurrentUser();
        List<Notification> notifications = user == null ? List.of() : notificationRepository.findForUser(user.getId());
        notificationList.getChildren().clear();
        if (notifications.isEmpty()) {
            notificationSummary.setText("No notifications yet.");
            addDemo("INBOX EMPTY", "You will see account, request, match, and inventory updates here.", "");
        } else {
            notificationSummary.setText(notifications.size() + " notification" + (notifications.size() == 1 ? "" : "s"));
            for (Notification notification : notifications) {
                addDemo(notification.getType().name(), notification.getMessage(), notification.getCreatedAt().toString());
            }
        }
    }

    private void addDemo(String type, String message, String time) {
        Label row = new Label(type + "\n" + message + "\n" + time);
        row.getStyleClass().add("notification-row");
        row.setMaxWidth(Double.MAX_VALUE);
        notificationList.getChildren().add(row);
    }

    @FXML public void goBack() { SceneManager.goToDashboard(); }
    @FXML public void openMap() { SceneManager.switchTo("map.fxml", "Nearby Network"); }
}
