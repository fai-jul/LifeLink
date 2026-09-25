package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.RequestStatus;
import com.lifelink.service.RequestQueueService;
import com.lifelink.util.DateUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Shows every active emergency request as a triage queue: a PriorityQueue
 * (see RequestQueueService) orders CRITICAL requests ahead of HIGH ahead
 * of NORMAL, with FIFO tie-breaking, so blood-bank staff always see the
 * most urgent case first regardless of arrival order.
 */
public class BloodBankRequestsController {

    @FXML private VBox requestQueueBox;
    @FXML private Label queueSummaryLabel;
    @FXML private Label emptyLabel;

    private final BloodRequestRepository repository = new BloodRequestRepository();
    private final RequestQueueService queueService = new RequestQueueService();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        List<BloodRequest> ordered = queueService.orderByPriority(repository.findActive());
        requestQueueBox.getChildren().clear();
        queueSummaryLabel.setText(ordered.size() + " active request" + (ordered.size() == 1 ? "" : "s")
                + " · ordered critical-first");
        emptyLabel.setVisible(ordered.isEmpty());
        emptyLabel.setManaged(ordered.isEmpty());

        int position = 1;
        for (BloodRequest request : ordered) {
            requestQueueBox.getChildren().add(buildRow(position++, request));
        }
    }

    private HBox buildRow(int position, BloodRequest request) {
        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().addAll("list-row-card", rowStyle(request.getUrgency().name()));
        row.setMaxWidth(Double.MAX_VALUE);

        Label posLabel = new Label(String.valueOf(position));
        posLabel.getStyleClass().add("queue-position");

        Label tag = new Label(request.getUrgency().name());
        tag.getStyleClass().addAll("list-row-tag", tagStyle(request.getUrgency().name()));

        VBox textBox = new VBox(2);
        Label title = new Label("#" + request.getId() + "  " + request.getBloodType().getLabel()
                + "  ·  " + request.getQuantity() + " unit(s)  ·  " + request.getStatus());
        title.getStyleClass().add("list-row-title");
        Label meta = new Label((request.getLocation() == null || request.getLocation().isBlank()
                ? "Location unavailable" : request.getLocation())
                + "  ·  requested " + DateUtil.format(request.getCreatedAt()));
        meta.getStyleClass().add("list-row-meta");
        textBox.getChildren().addAll(title, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button fulfillBtn = new Button("Mark Fulfilled");
        fulfillBtn.getStyleClass().add("btn-secondary");
        fulfillBtn.setDisable(request.getStatus() == RequestStatus.FULFILLED);
        fulfillBtn.setOnAction(e -> {
            if (!repository.findActive().stream().anyMatch(active -> active.getId() == request.getId())) {
                refresh();
                return;
            }
            int bankId = SceneManager.getCurrentUser().getId();
            repository.updateStatus(request.getId(), RequestStatus.FULFILLED,
                    "Marked fulfilled by blood bank #" + bankId + ".");
            notificationRepository.create(request.getRecipientId(),
                    "Your emergency blood request #" + request.getId() + " was fulfilled by a blood bank.",
                    com.lifelink.model.NotificationType.MATCH);
            activityLogRepository.log(bankId, "REQUEST_FULFILLED",
                    "Emergency request #" + request.getId() + " marked fulfilled.");
            refresh();
        });

        row.getChildren().addAll(posLabel, tag, textBox, fulfillBtn);
        return row;
    }

    private String rowStyle(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> "list-row-critical";
            case "HIGH" -> "list-row-high";
            default -> "list-row-normal";
        };
    }

    private String tagStyle(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> "tag-emergency";
            case "HIGH" -> "tag-expiry";
            default -> "tag-match";
        };
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }
}
