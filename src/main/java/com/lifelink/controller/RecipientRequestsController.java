package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.model.*;
import com.lifelink.service.EmergencyRequestService;
import com.lifelink.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class RecipientRequestsController {

    @FXML private ComboBox<BloodType> bloodTypeCombo;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private ComboBox<RequestPriority> priorityCombo;
    @FXML private TextField locationField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private ListView<BloodRequest> requestList;
    @FXML private VBox timelineBox;
    @FXML private Label errorLabel;

    private final EmergencyRequestService service = new EmergencyRequestService();
    private Recipient recipient;

    @FXML
    public void initialize() {
        recipient = (Recipient) SceneManager.getCurrentUser();
        bloodTypeCombo.setItems(FXCollections.observableArrayList(BloodType.values()));
        priorityCombo.setItems(FXCollections.observableArrayList(RequestPriority.values()));
        priorityCombo.setValue(RequestPriority.HIGH);
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        locationField.setText(recipient.getLocation());
        requestList.setItems(FXCollections.observableArrayList(service.requestsFor(recipient.getId())));
        requestList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(BloodRequest item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        "#" + item.getId() + " · " + item.getBloodType().getLabel() + " · "
                                + item.getQuantity() + " unit(s) · " + item.getStatus()
                                + " · " + DateUtil.format(item.getCreatedAt()));
            }
        });
        requestList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> showTimeline());
    }

    @FXML
    public void handleCreate() {
        try {
            double latitude = parseCoordinate(latitudeField.getText());
            double longitude = parseCoordinate(longitudeField.getText());
            if ((latitude != 0 && (latitude < -90 || latitude > 90))
                    || (longitude != 0 && (longitude < -180 || longitude > 180))) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90; longitude between -180 and 180.");
            }
            BloodRequest request = service.createAndMatch(
                    recipient.getId(), bloodTypeCombo.getValue(), quantitySpinner.getValue(),
                    locationField.getText(), latitude, longitude, priorityCombo.getValue());
            requestList.getItems().add(0, request);
            requestList.getSelectionModel().select(request);
            showTimeline();
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } catch (RuntimeException e) {
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    public void handleCancel() {
        BloodRequest request = requestList.getSelectionModel().getSelectedItem();
        if (request != null && request.getStatus() != RequestStatus.CANCELLED) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancel this emergency request?", ButtonType.CANCEL, ButtonType.OK);
            confirm.setHeaderText("Cancel request #" + request.getId());
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            service.cancel(request);
            request.setStatus(RequestStatus.CANCELLED);
            requestList.refresh();
            showTimeline();
        }
    }

    @FXML
    public void showTimeline() {
        timelineBox.getChildren().clear();
        BloodRequest selected = requestList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            timelineBox.getChildren().add(new Label("Select a request to view its status timeline."));
            return;
        }
        for (RequestStatusEvent event : service.timeline(selected.getId())) {
            Label label = new Label(event.getStatus() + "  ·  " + DateUtil.format(event.getTimestamp())
                    + "\n" + event.getNote());
            label.getStyleClass().add("timeline-entry");
            timelineBox.getChildren().add(label);
        }
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }

    private double parseCoordinate(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            double coordinate = Double.parseDouble(value.trim());
            if (!Double.isFinite(coordinate)) {
                throw new IllegalArgumentException("Coordinates must be finite numbers.");
            }
            return coordinate;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Latitude and longitude must be valid numbers.");
        }
    }
}
