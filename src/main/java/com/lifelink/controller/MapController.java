package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.BloodBank;
import com.lifelink.model.BloodRequest;
import com.lifelink.model.Donor;
import com.lifelink.model.Recipient;
import javafx.application.Platform;
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
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    // Fixed seed so the prototype city layout (roads/buildings/trees) stays
    // stable across re-renders/resizes instead of reshuffling every frame.
    private static final long BACKDROP_SEED = 42L;

    @FXML
    public void initialize() {
        mapLayer.getStyleClass().add("map-layer");
        mapLayer.setMinSize(0, 0);
        mapLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // BUG FIX: mapLayer was a *managed* child of the StackPane. A StackPane
        // resizes/repositions every managed child itself on each automatic
        // layout pass, which fights with the manual resizeRelocate() calls in
        // renderMap() below and can leave the layer at a stale/zero size (it
        // renders once at 0x0 before the first real layout pass runs, and
        // depending on layout timing may never get corrected afterwards).
        // Marking it unmanaged means only OUR code controls its size/position,
        // so the manual layout in renderMap() is authoritative and reliable.
        mapLayer.setManaged(false);

        mapViewport.getChildren().add(0, mapLayer);

        // BUG FIX: StackPane (and JavaFX Regions in general) do NOT clip their
        // children to their own bounds. When zoom > 100% scales mapLayer up,
        // the enlarged content was painting straight over the sidebar cards
        // and even the topbar instead of being cropped to the map panel.
        // Clipping mapViewport to its own size fixes that — the clip stays in
        // sync automatically via the width/height bindings below.
        Rectangle viewportClip = new Rectangle();
        viewportClip.widthProperty().bind(mapViewport.widthProperty());
        viewportClip.heightProperty().bind(mapViewport.heightProperty());
        mapViewport.setClip(viewportClip);

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

        // BUG FIX: at initialize() time the scene hasn't been laid out yet, so
        // mapViewport.getWidth()/getHeight() are still 0 and this first call
        // draws an empty/invisible frame. The width/height listeners above are
        // supposed to trigger the real redraw once layout happens, but on some
        // platforms/timings that first pass can race with FXML/CSS application
        // and get missed. Platform.runLater() guarantees one extra render pass
        // after the initial layout has definitely completed, so the map is
        // never left blank.
        renderMap();
        Platform.runLater(this::renderMap);
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
        if (width <= 0 || height <= 0) {
            // Nothing usable to lay out yet (pre-layout pass) — the
            // Platform.runLater()/property-listener calls will retry once the
            // viewport actually has a size, so just bail out instead of
            // drawing a degenerate 0x0 frame.
            return;
        }
        mapLayer.setPrefSize(width, height);
        mapLayer.resizeRelocate(0, 0, width, height);
        mapLayer.getChildren().clear();

        Label title = new Label("NETWORK MAP — PROTOTYPE CITY VIEW");
        title.getStyleClass().add("map-backdrop-label");
        title.setLayoutX(22);
        title.setLayoutY(20);
        mapLayer.getChildren().add(title);

        addCityBackdrop(width, height);

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

    /**
     * Draws a lightweight "prototype city" backdrop — a grid of streets with
     * blocks that each randomly get a building, a small tree cluster, or are
     * left as an open lot. It's deliberately flat 2D (rectangles + a
     * lighter/darker strip per building for a cheap beveled "3D block" look)
     * rather than a real JavaFX 3D scene (Box/SubScene/PerspectiveCamera):
     * that keeps it fast, dependency-free, and trivial to theme via CSS,
     * which is what a prototype map needs. Donor/recipient/blood-bank/request
     * pins are drawn afterwards in renderMap(), on top of this layer.
     */
    private void addCityBackdrop(double width, double height) {
        double margin = 46; // keep clear of the "NETWORK MAP" label and edges
        double usableWidth = width - margin * 2;
        double usableHeight = height - margin * 2 - 20;
        if (usableWidth < 80 || usableHeight < 80) return;

        int columns = Math.max(2, Math.min(8, (int) (usableWidth / 130)));
        int rows = Math.max(2, Math.min(6, (int) (usableHeight / 120)));
        double cellWidth = usableWidth / columns;
        double cellHeight = usableHeight / rows;
        double top = margin + 20;

        Random random = new Random(BACKDROP_SEED);

        double roadWidth = 10;
        Color asphalt = Color.web("#4a5560");
        for (int c = 0; c <= columns; c++) {
            Rectangle road = new Rectangle(roadWidth, usableHeight, asphalt);
            road.setLayoutX(margin + c * cellWidth - roadWidth / 2);
            road.setLayoutY(top);
            road.getStyleClass().add("map-road");
            mapLayer.getChildren().add(road);
        }
        for (int r = 0; r <= rows; r++) {
            Rectangle road = new Rectangle(usableWidth, roadWidth, asphalt);
            road.setLayoutX(margin);
            road.setLayoutY(top + r * cellHeight - roadWidth / 2);
            road.getStyleClass().add("map-road");
            mapLayer.getChildren().add(road);
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                double blockX = margin + c * cellWidth;
                double blockY = top + r * cellHeight;
                double roll = random.nextDouble();
                if (roll < 0.55) {
                    addBuilding(blockX, blockY, cellWidth, cellHeight, random);
                } else if (roll < 0.82) {
                    addTreeCluster(blockX, blockY, cellWidth, cellHeight, random);
                }
                // else: leave the block as open ground for visual breathing room
            }
        }
    }

    private void addBuilding(double blockX, double blockY, double blockW, double blockH, Random random) {
        double pad = Math.min(blockW, blockH) * 0.22;
        double bw = Math.max(16, blockW - pad * 2);
        double bh = Math.max(16, blockH - pad * 2);
        double bx = blockX + (blockW - bw) / 2;
        double by = blockY + (blockH - bh) / 2;

        Color[] palette = {
                Color.web("#b48a5a"), Color.web("#8f9fae"), Color.web("#a6693f"),
                Color.web("#5f7d8c"), Color.web("#7c8f6d"), Color.web("#9c7a8f")
        };
        Color wall = palette[random.nextInt(palette.length)];

        Rectangle body = new Rectangle(bw, bh, wall);
        body.setLayoutX(bx);
        body.setLayoutY(by);
        body.getStyleClass().add("map-building");

        // Lighter strip along the top and a darker strip along the right edge
        // give a cheap beveled/"3D block" read without real 3D geometry.
        Rectangle roof = new Rectangle(bw, Math.max(5, bh * 0.16), wall.deriveColor(0, 1, 1.35, 1));
        roof.setLayoutX(bx);
        roof.setLayoutY(by);
        roof.getStyleClass().add("map-building-roof");

        double shadeWidth = Math.max(4, bw * 0.18);
        Rectangle shade = new Rectangle(shadeWidth, bh, wall.deriveColor(0, 1, 0.55, 1));
        shade.setLayoutX(bx + bw - shadeWidth);
        shade.setLayoutY(by);
        shade.getStyleClass().add("map-building-shade");

        mapLayer.getChildren().addAll(body, roof, shade);
    }

    private void addTreeCluster(double blockX, double blockY, double blockW, double blockH, Random random) {
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double tx = blockX + 14 + random.nextDouble() * Math.max(1, blockW - 28);
            double ty = blockY + 14 + random.nextDouble() * Math.max(1, blockH - 28);
            addTree(tx, ty, random);
        }
    }

    private void addTree(double x, double y, Random random) {
        double trunkHeight = 6 + random.nextDouble() * 3;
        Rectangle trunk = new Rectangle(3, trunkHeight, Color.web("#8a6a4a"));
        trunk.setLayoutX(x - 1.5);
        trunk.setLayoutY(y);
        trunk.getStyleClass().add("map-tree-trunk");

        double radius = 6 + random.nextDouble() * 4;
        Circle foliage = new Circle(radius, Color.web("#4f8f5f"));
        foliage.setLayoutX(x);
        foliage.setLayoutY(y - radius * 0.6);
        foliage.getStyleClass().add("map-tree-foliage");

        mapLayer.getChildren().addAll(trunk, foliage);
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