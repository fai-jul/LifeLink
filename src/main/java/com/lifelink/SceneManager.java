package com.lifelink;

import com.lifelink.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.paint.Color;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Tiny singleton that owns the primary Stage and knows how to swap in a
 * new FXML-based scene. Also holds the currently logged-in user for the
 * duration of the session (no multi-window/multi-session support needed
 * for this project).
 */
public final class SceneManager {

    private static Stage primaryStage;
    private static User currentUser;

    private SceneManager() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
        // Keep JavaFX's fullscreen exit shortcut without displaying its native hint.
        primaryStage.setFullScreenExitHint("");
    }

    public static Stage getStage() {
        return primaryStage;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /** Loads the given FXML (relative to /com/lifelink/fxml/) and shows it. */
    public static void switchTo(String fxmlFileName, String title) {
        try {
            URL fxmlUrl = SceneManager.class.getResource("/com/lifelink/fxml/" + fxmlFileName);
            if (fxmlUrl == null) {
                throw new IllegalStateException("FXML not found on classpath: " + fxmlFileName);
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1180, 760);
            scene.getStylesheets().add(SceneManager.class.getResource("/com/lifelink/css/app.css").toExternalForm());
            if ("splash.fxml".equals(fxmlFileName)) {
                scene.setFill(Color.web("#081b2d"));
            }

            boolean wasMaximized = primaryStage.isMaximized();
            primaryStage.setScene(scene);
            primaryStage.setTitle("LifeLink" + (title != null ? " - " + title : ""));
            if (!primaryStage.isFullScreen() && !wasMaximized) {
                primaryStage.centerOnScreen();
            }
            if (wasMaximized) {
                primaryStage.setMaximized(true);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load screen: " + fxmlFileName, e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to initialize screen " + fxmlFileName + ": "
                    + rootCauseMessage(e), e);
        }
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    /** Routes to the correct dashboard FXML based on the current user's role. */
    public static void goToDashboard() {
        if (currentUser == null) {
            switchTo("login.fxml", "Login");
            return;
        }
        switch (currentUser.getRole()) {
            case DONOR -> switchTo("donor_dashboard.fxml", "Donor Dashboard");
            case RECIPIENT -> switchTo("recipient_dashboard.fxml", "Recipient Dashboard");
            case BLOOD_BANK -> switchTo("bloodbank_dashboard.fxml", "Blood Bank Dashboard");
                case DOCTOR, LAB_TECHNICIAN, ADMINISTRATOR, RECEPTIONIST ->
                    switchTo("staff_dashboard.fxml", currentUser.getDashboardTitle());
        }
    }

    public static void logout() {
        currentUser = null;
        switchTo("login.fxml", "Login");
    }
}
