package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.ActivityLogRepository;
import com.lifelink.model.ActivityLog;
import com.lifelink.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ActivityLogController {

    @FXML private Label titleLabel;
    @FXML private TableView<ActivityLog> logTable;
    @FXML private TableColumn<ActivityLog, String> actionColumn;
    @FXML private TableColumn<ActivityLog, String> descriptionColumn;
    @FXML private TableColumn<ActivityLog, String> timestampColumn;

    private final ActivityLogRepository repository = new ActivityLogRepository();

    @FXML
    public void initialize() {
        var user = SceneManager.getCurrentUser();
        titleLabel.setText(user.getName() + " · Activity Log");
        actionColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getAction()));
        descriptionColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDescription()));
        timestampColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.format(data.getValue().getTimestamp())));
        logTable.setItems(FXCollections.observableArrayList(repository.forUser(user.getId(), 250)));
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }
}
