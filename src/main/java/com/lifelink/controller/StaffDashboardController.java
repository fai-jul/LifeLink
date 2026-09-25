package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.model.User;
import com.lifelink.service.AccessControlService;
import com.lifelink.service.Permission;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class StaffDashboardController {

    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private Label permissionsLabel;

    private final AccessControlService accessControl = new AccessControlService();

    @FXML
    public void initialize() {
        User user = SceneManager.getCurrentUser();
        nameLabel.setText(user.getName());
        roleLabel.setText(user.getDashboardTitle());
        String permissions = java.util.Arrays.stream(Permission.values())
                .filter(permission -> accessControl.can(user, permission))
                .map(Enum::name)
                .map(value -> value.replace('_', ' '))
                .reduce((left, right) -> left + "  ·  " + right)
                .orElse("No permissions assigned");
        permissionsLabel.setText(permissions);
    }

    @FXML
    public void openActivity() {
        SceneManager.switchTo("activity_log.fxml", "Activity Log");
    }

    @FXML
    public void handleLogout() {
        SceneManager.logout();
    }
}