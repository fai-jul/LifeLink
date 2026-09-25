package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.service.AuthenticationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthenticationService authService = new AuthenticationService();

    @FXML
    public void handleLogin() {
        AuthenticationService.AuthResult result = authService.login(emailField.getText(), passwordField.getText());
        if (!result.success) {
            showError(result.message);
            return;
        }
        SceneManager.setCurrentUser(result.user);
        SceneManager.goToDashboard();
    }

    @FXML
    public void goToSignup() {
        SceneManager.switchTo("signup.fxml", "Sign Up");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
