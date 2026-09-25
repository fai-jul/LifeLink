package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.model.*;
import com.lifelink.service.InventoryService;
import com.lifelink.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;

public class InventoryController {

    @FXML private TextField searchField;
    @FXML private ComboBox<BloodType> typeFilter;
    @FXML private ComboBox<BloodUnitStatus> statusFilter;
    @FXML private TableView<BloodUnit> inventoryTable;
    @FXML private TableColumn<BloodUnit, String> typeColumn;
    @FXML private TableColumn<BloodUnit, String> componentColumn;
    @FXML private TableColumn<BloodUnit, String> storageColumn;
    @FXML private TableColumn<BloodUnit, Number> quantityColumn;
    @FXML private TableColumn<BloodUnit, String> collectionColumn;
    @FXML private TableColumn<BloodUnit, String> expiryColumn;
    @FXML private TableColumn<BloodUnit, String> statusColumn;
    @FXML private Label titleLabel;
    @FXML private Label errorLabel;
    @FXML private ComboBox<BloodType> formType;
    @FXML private ComboBox<BloodComponent> formComponent;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private DatePicker collectionPicker;
    @FXML private DatePicker expiryPicker;
    @FXML private TextField storageField;

    private final InventoryService service = new InventoryService();
    private BloodBank bank;
    private BloodUnit editing;

    @FXML
    public void initialize() {
        bank = (BloodBank) SceneManager.getCurrentUser();
        titleLabel.setText(bank.getName() + " · Inventory");
        typeFilter.setItems(FXCollections.observableArrayList(BloodType.values()));
        statusFilter.setItems(FXCollections.observableArrayList(BloodUnitStatus.values()));
        formType.setItems(FXCollections.observableArrayList(BloodType.values()));
        formComponent.setItems(FXCollections.observableArrayList(BloodComponent.values()));
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1));
        typeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getBloodType().getLabel()));
        componentColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getComponent().name()));
        storageColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getStorageLocation()));
        quantityColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(
                data.getValue().getQuantity()));
        collectionColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.format(data.getValue().getCollectionDate())));
        expiryColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                DateUtil.format(data.getValue().getExpiryDate())));
        statusColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStatus().name()));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refresh());
        typeFilter.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    private void refresh() {
        if (bank != null) {
            inventoryTable.setItems(FXCollections.observableArrayList(
                    service.search(bank.getId(), searchField.getText(), typeFilter.getValue(), statusFilter.getValue())));
        }
    }

    @FXML
    public void handleSave() {
        try {
            BloodType type = formType.getValue();
            BloodComponent component = formComponent.getValue();
            int quantity = quantitySpinner.getValue();
            LocalDate collection = collectionPicker.getValue();
            LocalDate expiry = expiryPicker.getValue();
            if (editing == null) {
                service.add(bank.getId(), type, component, quantity, collection, expiry, storageField.getText());
            } else {
                editing.setBloodType(type);
                editing.setComponent(component);
                editing.setQuantity(quantity);
                editing.setCollectionDate(collection);
                editing.setExpiryDate(expiry);
                editing.setStorageLocation(storageField.getText());
                service.update(editing);
            }
            clearForm();
            refresh();
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleEdit() {
        BloodUnit selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an inventory item to edit.");
            return;
        }
        editing = selected;
        formType.setValue(selected.getBloodType());
        formComponent.setValue(selected.getComponent());
        quantitySpinner.getValueFactory().setValue(selected.getQuantity());
        collectionPicker.setValue(selected.getCollectionDate());
        expiryPicker.setValue(selected.getExpiryDate());
        storageField.setText(selected.getStorageLocation());
    }

    @FXML
    public void handleRemove() {
        BloodUnit selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an inventory item to remove.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove this inventory item?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Remove inventory");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            service.remove(selected);
            refresh();
            clearForm();
        }
    }

    @FXML
    public void handleClear() {
        clearForm();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }

    private void clearForm() {
        editing = null;
        formType.setValue(null);
        formComponent.setValue(null);
        quantitySpinner.getValueFactory().setValue(1);
        collectionPicker.setValue(null);
        expiryPicker.setValue(null);
        storageField.clear();
    }

    private void showError(String message) {
        errorLabel.setText(message == null ? "Unable to save inventory." : message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
