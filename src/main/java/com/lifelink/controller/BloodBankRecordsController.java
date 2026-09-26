package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodBankTrackingRepository;
import com.lifelink.model.BloodBank;
import com.lifelink.model.BloodBankDonorRecord;
import com.lifelink.model.BloodBankPatientRecord;
import com.lifelink.service.ApiClientService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class BloodBankRecordsController {

    @FXML private TextField apiUrlField;
    @FXML private Label syncStatusLabel;
    @FXML private TableView<BloodBankDonorRecord> donorTable;
    @FXML private TableColumn<BloodBankDonorRecord, String> donorNameColumn;
    @FXML private TableColumn<BloodBankDonorRecord, String> donorBloodTypeColumn;
    @FXML private TableColumn<BloodBankDonorRecord, String> donorLocationColumn;
    @FXML private TableColumn<BloodBankDonorRecord, String> donorStatusColumn;
    @FXML private TableColumn<BloodBankDonorRecord, String> donorPhoneColumn;

    @FXML private TableView<BloodBankPatientRecord> patientTable;
    @FXML private TableColumn<BloodBankPatientRecord, String> patientNameColumn;
    @FXML private TableColumn<BloodBankPatientRecord, String> patientBloodTypeColumn;
    @FXML private TableColumn<BloodBankPatientRecord, String> patientLocationColumn;
    @FXML private TableColumn<BloodBankPatientRecord, String> patientStatusColumn;
    @FXML private TableColumn<BloodBankPatientRecord, String> patientPhoneColumn;

    private final ApiClientService apiClientService = new ApiClientService();
    private final BloodBankTrackingRepository trackingRepository = new BloodBankTrackingRepository();
    private BloodBank bloodBank;

    @FXML
    public void initialize() {
        bloodBank = (BloodBank) SceneManager.getCurrentUser();
        apiUrlField.setText(apiClientService.getConfiguredBaseUrl());

        donorNameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getName()));
        donorBloodTypeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getBloodType()));
        donorLocationColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getLocation()));
        donorStatusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));
        donorPhoneColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getPhone()));

        patientNameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getName()));
        patientBloodTypeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getBloodType()));
        patientLocationColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getLocation()));
        patientStatusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));
        patientPhoneColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getPhone()));

        refreshRecords();
    }

    @FXML
    public void handleSync() {
        if (bloodBank == null) {
            syncStatusLabel.setText("No blood bank session found.");
            return;
        }

        String url = apiUrlField.getText();
        if (url == null || url.isBlank()) {
            syncStatusLabel.setText("Please enter the API base URL first.");
            return;
        }

        try {
            ApiClientService.SyncResult result = apiClientService.syncBloodBankData(bloodBank.getId(), url);
            refreshRecords();
            syncStatusLabel.setText("Synced " + result.donorsSynced() + " donor records and " + result.patientsSynced() + " patient records.");
        } catch (RuntimeException e) {
            syncStatusLabel.setText("Sync failed: " + e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }

    private void refreshRecords() {
        if (bloodBank == null) {
            donorTable.setItems(FXCollections.observableArrayList());
            patientTable.setItems(FXCollections.observableArrayList());
            syncStatusLabel.setText("Blood bank session unavailable.");
            return;
        }

        donorTable.setItems(FXCollections.observableArrayList(trackingRepository.findDonorsForBank(bloodBank.getId())));
        patientTable.setItems(FXCollections.observableArrayList(trackingRepository.findPatientsForBank(bloodBank.getId())));

        if (donorTable.getItems().isEmpty() && patientTable.getItems().isEmpty()) {
            syncStatusLabel.setText("No synced records yet. Use the API sync button to load donor/patient data.");
        }
    }
}
