package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class DonorRequestsController {

    @FXML private Label summaryLabel;
    @FXML private VBox requestBox;

    private final BloodRequestRepository repository = new BloodRequestRepository();
    private Donor donor;

    @FXML
    public void initialize() {
        donor = (Donor) SceneManager.getCurrentUser();
        List<BloodRequest> requests = repository.findActive().stream()
                .filter(request -> donor.getBloodType().canDonateTo(request.getBloodType()))
                .toList();
        summaryLabel.setText(requests.size() + " compatible request" + (requests.size() == 1 ? "" : "s"));
        for (BloodRequest request : requests) {
            Label card = new Label(request.getUrgency() + "  /  " + request.getBloodType().getLabel()
                    + "  /  " + request.getQuantity() + " unit(s)\n"
                    + (request.getLocation() == null ? "Location unavailable" : request.getLocation()));
            card.getStyleClass().add("timeline-entry");
            card.setMaxWidth(Double.MAX_VALUE);
            requestBox.getChildren().add(card);
        }
        if (requests.isEmpty()) {
            Label empty = new Label("No compatible emergency requests are waiting right now.");
            empty.getStyleClass().add("empty-state");
            requestBox.getChildren().add(empty);
        }
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }
}
