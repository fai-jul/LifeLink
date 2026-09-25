package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.BloodBank;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
import com.lifelink.model.Recipient;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;

public class MapController {
    @FXML private VBox markerList;
    @FXML private StackPane mapViewport;
    @FXML private Label networkSummary;
    @FXML private Label selectedMarkerLabel;
    @FXML private Label zoomLabel;

    private final UserRepository userRepository = new UserRepository();
    private final BloodRequestRepository requestRepository = new BloodRequestRepository();
    private final Pane mapLayer = new Pane();
    private List<MapMarker> currentMarkers = List.of();
    private double zoom = 1;

    @FXML
    public void initialize() {
        mapLayer.getStyleClass().add("map-layer");
        mapLayer.setMinSize(0, 0);
        mapLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        mapViewport.getChildren().add(0, mapLayer);
        try {
            loadMarkers();
        } catch (RuntimeException exception) {
            currentMarkers = List.of();
            markerList.getChildren().clear();
            Label error = new Label("Map data is temporarily unavailable.\n" + rootCauseMessage(exception));
            error.getStyleClass().add("map-empty-state");
            error.setWrapText(true);
            markerList.getChildren().add(error);
            networkSummary.setText("Unable to load network locations.");
        }
        mapViewport.widthProperty().addListener((obs, oldValue, newValue) -> renderMap());
        mapViewport.heightProperty().addListener((obs, oldValue, newValue) -> renderMap());
        renderMap();
    }

    @FXML public void goBack() { SceneManager.goToDashboard(); }

    @FXML public void openStatistics() { SceneManager.switchTo("statistics.fxml", "Statistics"); }
    @FXML public void openNotifications() { SceneManager.switchTo("notifications.fxml", "Notifications"); }
    @FXML public void zoomIn() { setZoom(zoom + 0.2); }
    @FXML public void zoomOut() { setZoom(zoom - 0.2); }

    @FXML
    public void resetMap() {
        setZoom(1);
        renderMap();
    }

    private void loadMarkers() {
        markerList.getChildren().clear();
        List<MapMarker> markers = new ArrayList<>();
        int donorIndex = 0;
        for (Donor donor : userRepository.findAllDonors(true)) {
            markers.add(withFallbackCoordinates(new MapMarker("DONOR", donor.getName(), donor.getLocation(),
                    donor.getLatitude(), donor.getLongitude(), Color.web("#167b5b")), donorIndex++));
        }
        int recipientIndex = 0;
        for (Recipient recipient : userRepository.findAllRecipients()) {
            markers.add(withFallbackCoordinates(new MapMarker("RECIPIENT", recipient.getName(),
                    recipient.getLocation(), recipient.getLatitude(), recipient.getLongitude(),
                    Color.web("#d06b3c")), recipientIndex++ + 2));
        }
        int bankIndex = 0;
        for (BloodBank bank : userRepository.findAllBloodBanks()) {
            markers.add(withFallbackCoordinates(new MapMarker("BLOOD BANK", bank.getName(), bank.getAddress(),
                    bank.getLatitude(), bank.getLongitude(), Color.web("#2c6bb0")), bankIndex++ + 4));
        }
        for (BloodRequest request : requestRepository.findActive()) {
            markers.add(withFallbackCoordinates(new MapMarker("NEEDS BLOOD", request.getBloodType().getLabel()
                    + " request", request.getLocation(), request.getLatitude(), request.getLongitude(),
                    Color.web("#d6473f")), markers.size() + 6));
        }
        currentMarkers = markers;

        long mapped = markers.stream().filter(MapController::hasCoordinates).count();
        networkSummary.setText(markers.size() + " active locations\n" + mapped + " plotted on the map\n"
                + (mapped == 0 ? "Add a location to see it here." : "Select a marker for details."));
        for (MapMarker marker : markers) {
            addMarkerRow(marker);
        }
        if (markers.isEmpty()) {
            Label empty = new Label("No active donors, banks, or requests yet.");
            empty.getStyleClass().add("empty-state");
            markerList.getChildren().add(empty);
        }
    }

    private void addMarkerRow(MapMarker marker) {
        String location = marker.detail() == null || marker.detail().isBlank()
                ? "Location not provided" : marker.detail();
        Label row = new Label(marker.kind() + "  /  " + marker.title() + "\n" + location
                + (hasCoordinates(marker.latitude(), marker.longitude()) ? "" : "\nNot plotted: coordinates missing"));
        row.getStyleClass().add("timeline-entry");
        row.setMaxWidth(Double.MAX_VALUE);
        row.setWrapText(true);
        row.setOnMouseClicked(event -> selectMarker(marker));
        markerList.getChildren().add(row);
    }

    private void renderMap() {
        if (mapViewport == null) return;
        double width = Math.max(0, mapViewport.getWidth());
        double height = Math.max(0, mapViewport.getHeight());
        mapLayer.setPrefSize(width, height);
        mapLayer.resizeRelocate(0, 0, width, height);
        mapLayer.getChildren().clear();

        Label title = new Label("NETWORK MAP");
        title.getStyleClass().add("map-backdrop-label");
        title.setLayoutX(22);
        title.setLayoutY(20);
        mapLayer.getChildren().add(title);

        addMapGrid(width, height);
        List<MapMarker> plotted = currentMarkers.stream()
                .filter(marker -> hasCoordinates(marker.latitude(), marker.longitude())).toList();
        if (plotted.isEmpty()) {
            Label empty = new Label("No saved coordinates to plot\nUse a location with latitude and longitude.");
            empty.getStyleClass().add("map-empty-state");
            empty.setAlignment(Pos.CENTER);
            empty.setLayoutX(Math.max(20, width / 2 - 150));
            empty.setLayoutY(Math.max(50, height / 2 - 30));
            mapLayer.getChildren().add(empty);
            return;
        }

        double minLat = plotted.stream().mapToDouble(MapMarker::latitude).min().orElse(0);
        double maxLat = plotted.stream().mapToDouble(MapMarker::latitude).max().orElse(0);
        double minLon = plotted.stream().mapToDouble(MapMarker::longitude).min().orElse(0);
        double maxLon = plotted.stream().mapToDouble(MapMarker::longitude).max().orElse(0);
        double horizontalPadding = 58;
        double verticalPadding = 68;
        double usableWidth = Math.max(1, width - horizontalPadding * 2);
        double usableHeight = Math.max(1, height - verticalPadding * 2);
        double lonRange = Math.max(0.000001, maxLon - minLon);
        double latRange = Math.max(0.000001, maxLat - minLat);

        for (MapMarker marker : plotted) {
            double x = horizontalPadding + ((marker.longitude() - minLon) / lonRange) * usableWidth;
            double y = verticalPadding + ((maxLat - marker.latitude()) / latRange) * usableHeight;
            addMapMarker(marker, x, y);
        }
    }

    private void addMapGrid(double width, double height) {
        for (int i = 1; i < 6; i++) {
            Label vertical = new Label();
            vertical.getStyleClass().add("map-grid-line-vertical");
            vertical.setLayoutX(width * i / 6);
            vertical.setLayoutY(42);
            vertical.setPrefHeight(Math.max(0, height - 84));
            mapLayer.getChildren().add(vertical);

            Label horizontal = new Label();
            horizontal.getStyleClass().add("map-grid-line-horizontal");
            horizontal.setLayoutX(30);
            horizontal.setLayoutY(height * i / 6);
            horizontal.setPrefWidth(Math.max(0, width - 60));
            mapLayer.getChildren().add(horizontal);
        }
    }

    private void addMapMarker(MapMarker marker, double x, double y) {
        Circle pin = new Circle(9, marker.color());
        pin.getStyleClass().add("map-pin");
        pin.setCursor(Cursor.HAND);
        pin.setLayoutX(x);
        pin.setLayoutY(y);
        pin.setOnMouseClicked(event -> selectMarker(marker));
        Tooltip.install(pin, new Tooltip(marker.kind() + " / " + marker.title()));
        mapLayer.getChildren().add(pin);

        Label label = new Label(marker.kind() + "\n" + marker.title());
        label.getStyleClass().add("map-marker-label");
        label.setLayoutX(x + 13);
        label.setLayoutY(y - 16);
        label.setOnMouseClicked(event -> selectMarker(marker));
        mapLayer.getChildren().add(label);
    }

    private void selectMarker(MapMarker marker) {
        String location = marker.detail() == null || marker.detail().isBlank()
                ? "Location not provided" : marker.detail();
        selectedMarkerLabel.setText(marker.kind() + "\n" + marker.title() + "\n" + location);
    }

    private void setZoom(double value) {
        zoom = Math.max(0.8, Math.min(1.6, value));
        mapLayer.setScaleX(zoom);
        mapLayer.setScaleY(zoom);
        zoomLabel.setText(Math.round(zoom * 100) + "%");
    }

    private static boolean hasCoordinates(MapMarker marker) {
        return hasCoordinates(marker.latitude(), marker.longitude());
    }

    private static boolean hasCoordinates(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude != 0 && longitude != 0
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    private MapMarker withFallbackCoordinates(MapMarker marker, int index) {
        if (hasCoordinates(marker)) return marker;
        double column = index % 4;
        double row = index / 4;
        return new MapMarker(marker.kind(), marker.title(), marker.detail(),
                23.72 + row * 0.06 + column * 0.008,
                90.32 + column * 0.08 + row * 0.012,
                marker.color());
    }

    private static String rootCauseMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private record MapMarker(String kind, String title, String detail, double latitude, double longitude, Color color) { }
}
