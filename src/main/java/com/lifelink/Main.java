package com.lifelink;

import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.DatabaseManager;
import com.lifelink.db.NotificationRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.service.EligibilityReminderMonitor;
import com.lifelink.service.EligibilityService;
import com.lifelink.service.InventoryExpiryMonitor;
import com.lifelink.service.InventoryService;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    private InventoryExpiryMonitor expiryMonitor;
    private EligibilityReminderMonitor eligibilityMonitor;

    @Override
    public void start(Stage primaryStage) {
        // Touch the DB layer on startup so schema creation happens before any UI needs it.
        DatabaseManager.getConnection();
        expiryMonitor = new InventoryExpiryMonitor(new InventoryService(), null);
        expiryMonitor.start();

        eligibilityMonitor = new EligibilityReminderMonitor(
                new UserRepository(), new NotificationRepository(),
                new ActivityLogRepository(), new EligibilityService(), null);
        eligibilityMonitor.start();

        SceneManager.init(primaryStage);
        primaryStage.setTitle("LifeLink");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(680);
        SceneManager.switchTo("splash.fxml", null);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (expiryMonitor != null) {
            expiryMonitor.close();
        }
        if (eligibilityMonitor != null) {
            eligibilityMonitor.close();
        }
        DatabaseManager.close();
    }

    public static void main(String[] args) {
        launch(args);
        System.out.println("Project is running");
    }
}
