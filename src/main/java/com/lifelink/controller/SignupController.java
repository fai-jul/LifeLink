package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.model.BloodType;
import com.lifelink.service.AuthenticationService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SignupController {

    @FXML private HBox roleToggleBox;
    @FXML private ToggleButton donorToggle;
    @FXML private ToggleButton recipientToggle;
    @FXML private ToggleButton bankToggle;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField locationField;

    @FXML private VBox donorFieldsBox;
    @FXML private ComboBox<BloodType> bloodTypeCombo;
    @FXML private TextField ageField;
    @FXML private TextField weightField;

    @FXML private VBox bankFieldsBox;
    @FXML private TextField addressField;

    @FXML private Label errorLabel;

    private final AuthenticationService authService = new AuthenticationService();
    private final ToggleGroup roleGroup = new ToggleGroup();

    @FXML
    public void initialize() {
        donorToggle.setToggleGroup(roleGroup);
        recipientToggle.setToggleGroup(roleGroup);
        bankToggle.setToggleGroup(roleGroup);

        bloodTypeCombo.setItems(FXCollections.observableArrayList(BloodType.values()));

        roleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                // Prevent deselecting all toggles - keep at least one selected.
                oldT.setSelected(true);
                return;
            }
            updateVisibleFields();
        });
        updateVisibleFields();
    }

    private void updateVisibleFields() {
        boolean isDonor = donorToggle.isSelected();
        boolean isBank = bankToggle.isSelected();

        donorFieldsBox.setVisible(isDonor);
        donorFieldsBox.setManaged(isDonor);

        bankFieldsBox.setVisible(isBank);
        bankFieldsBox.setManaged(isBank);
    }

    @FXML
    public void handleSignup() {
        AuthenticationService.AuthResult result;

        if (donorToggle.isSelected()) {
            int age = parseIntSafe(ageField.getText());
            double weight = parseDoubleSafe(weightField.getText());
            result = authService.registerDonor(
                    nameField.getText(), emailField.getText(), passwordField.getText(), phoneField.getText(),
                    locationField.getText(), bloodTypeCombo.getValue(), age, weight
            );
        } else if (bankToggle.isSelected()) {
            result = authService.registerBloodBank(
                    nameField.getText(), emailField.getText(), passwordField.getText(), phoneField.getText(),
                    locationField.getText(), addressField.getText()
            );
        } else {
            result = authService.registerRecipient(
                    nameField.getText(), emailField.getText(), passwordField.getText(), phoneField.getText(),
                    locationField.getText()
            );
        }

        if (!result.success) {
            showError(result.message);
            return;
        }

        SceneManager.setCurrentUser(result.user);
        SceneManager.goToDashboard();
    }

    @FXML
    public void goToLogin() {
        SceneManager.switchTo("login.fxml", "Login");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private double parseDoubleSafe(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
