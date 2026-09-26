package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.BloodBank;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
import com.lifelink.model.Recipient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class MapController {
    @FXML private VBox markerList;
    @FXML private Label networkSummary;
    @FXML private Label selectedMarkerLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;

    private final UserRepository userRepository = new UserRepository();
    private final BloodRequestRepository requestRepository = new BloodRequestRepository();
    private List<NetworkEntry> allEntries = List.of();

    @FXML
    public void initialize() {
        categoryFilter.getItems().setAll("All records", "Donors", "Recipients", "Blood banks", "Blood requests");
        categoryFilter.getSelectionModel().selectFirst();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshEntries());
        categoryFilter.valueProperty().addListener((obs, oldValue, newValue) -> refreshEntries());

        try {
            loadMarkers();
        } catch (RuntimeException exception) {
            allEntries = List.of();
            markerList.getChildren().clear();
            Label error = new Label("Map data is temporarily unavailable.\n" + rootCauseMessage(exception));
            error.getStyleClass().add("map-empty-state");
            error.setWrapText(true);
            markerList.getChildren().add(error);
            networkSummary.setText("Unable to load network locations.");
        }
    }

    @FXML public void goBack() { SceneManager.goToDashboard(); }

    @FXML public void openStatistics() { SceneManager.switchTo("statistics.fxml", "Statistics"); }
    @FXML public void openNotifications() { SceneManager.switchTo("notifications.fxml", "Notifications"); }
    private void loadMarkers() {
        markerList.getChildren().clear();
        List<NetworkEntry> entries = new ArrayList<>();
        for (Donor donor : userRepository.findAllDonors(true)) {
            entries.add(new NetworkEntry("Donor", donor.getName(), donor.getBloodType().getLabel(),
                    donor.getLocation(), donor.getPhone(), donor.isAvailable() ? "Available" : "Unavailable", Color.web("#167b5b")));
        }
        for (Recipient recipient : userRepository.findAllRecipients()) {
            entries.add(new NetworkEntry("Recipient", recipient.getName(), "Blood request", recipient.getLocation(),
                    recipient.getPhone(), "Registered", Color.web("#d06b3c")));
        }
        for (BloodBank bank : userRepository.findAllBloodBanks()) {
            entries.add(new NetworkEntry("Blood bank", bank.getName(), "Blood storage and requests",
                    bank.getAddress(), bank.getPhone(), "Operational", Color.web("#2c6bb0")));
        }
        for (BloodRequest request : requestRepository.findActive()) {
            entries.add(new NetworkEntry("Blood request", request.getBloodType().getLabel() + " needed",
                    request.getQuantity() + " unit(s) · " + request.getUrgency().name(), request.getLocation(),
                    "Request #" + request.getId(), request.getStatus().name(), Color.web("#d6473f")));
        }
        allEntries = entries;
        refreshEntries();
    }

    @FXML
    public void refreshDirectory() {
        loadMarkers();
    }

    private void refreshEntries() {
        if (markerList == null) return;
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = categoryFilter.getValue();
        List<NetworkEntry> filtered = allEntries.stream()
                .filter(entry -> category == null || "All records".equals(category) || entry.category().equals(category.substring(0, category.length() - 1)))
                .filter(entry -> query.isBlank() || entry.searchText().contains(query))
                .toList();
        markerList.getChildren().clear();
        networkSummary.setText(filtered.size() + " matching records\n" + allEntries.size() + " total network records\n"
                + (query.isBlank() ? "Use the search and filters to browse the network." : "Showing filtered results."));
        for (NetworkEntry entry : filtered) addEntryRow(entry);
        if (filtered.isEmpty()) {
            Label empty = new Label(allEntries.isEmpty() ? "No network records are available yet." : "No records match your search.");
            empty.getStyleClass().add("empty-state");
            markerList.getChildren().add(empty);
        }
    }

    private void addEntryRow(NetworkEntry entry) {
        VBox row = new VBox(4);
        row.getStyleClass().add("network-entry");
        Label heading = new Label(entry.category().toUpperCase() + "  ·  " + entry.name());
        heading.getStyleClass().add("network-entry-title");
        Label details = new Label(entry.bloodInfo() + "\n" + entry.locationOrFallback()
                + "\n" + entry.phoneOrReference() + "  ·  " + entry.status());
        details.getStyleClass().add("network-entry-detail");
        details.setWrapText(true);
        row.getChildren().addAll(heading, details);
        row.setOnMouseClicked(event -> selectedMarkerLabel.setText(entry.category() + "\n"
                + entry.name() + "\n" + entry.locationOrFallback() + "\n" + entry.phoneOrReference()));
        markerList.getChildren().add(row);
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private record NetworkEntry(String category, String name, String bloodInfo, String location,
                                String phoneOrReference, String status, Color color) {
        private String locationOrFallback() {
            return location == null || location.isBlank() ? "Location not provided" : location;
        }

        private String searchText() {
            return (category + " " + name + " " + bloodInfo + " " + locationOrFallback()
                    + " " + phoneOrReference + " " + status).toLowerCase();
        }
    }
}