package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.DonationRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.Donation;
import com.lifelink.model.Donor;
import com.lifelink.service.EligibilityService;
import com.lifelink.util.DateUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class DonationHistoryController {

    @FXML private Label donationCountLabel;
    @FXML private Label unitsLabel;
    @FXML private Label bloodTypeLabel;
    @FXML private Label nextEligibleLabel;
    @FXML private Label emptyLabel;
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private VBox donationListBox;

    private final DonationRepository repository = new DonationRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();
    private final EligibilityService eligibilityService = new EligibilityService();
    private Donor donor;
    private List<Donation> currentDonations = List.of();

    @FXML
    public void initialize() {
        donor = (Donor) SceneManager.getCurrentUser();
        bloodTypeLabel.setText(donor.getBloodType().getLabel());
        datePicker.setValue(LocalDate.now());
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        refresh();
    }

    private void refresh() {
        currentDonations = repository.findForDonor(donor.getId());
        donationListBox.getChildren().clear();
        donationCountLabel.setText(String.valueOf(currentDonations.size()));
        unitsLabel.setText(String.valueOf(currentDonations.stream().mapToInt(Donation::getQuantity).sum()));
        nextEligibleLabel.setText(eligibilityService.isEligible(donor)
                ? "Eligible now" : DateUtil.format(eligibilityService.nextEligibleDate(donor)));
        emptyLabel.setVisible(currentDonations.isEmpty());
        emptyLabel.setManaged(currentDonations.isEmpty());
        for (Donation donation : currentDonations) {
            donationListBox.getChildren().add(buildDonationRow(donation));
        }
    }

    private HBox buildDonationRow(Donation donation) {
        HBox row = new HBox(12);
        row.getStyleClass().addAll("list-row-card", "list-row-normal");
        row.setMaxWidth(Double.MAX_VALUE);

        Label tag = new Label(donation.getBloodType().getLabel());
        tag.getStyleClass().addAll("list-row-tag", "tag-match");

        VBox textBox = new VBox(2);
        Label title = new Label(donation.getQuantity() + " unit(s) donated");
        title.getStyleClass().add("list-row-title");
        Label meta = new Label(DateUtil.format(donation.getDonationDate()));
        meta.getStyleClass().add("list-row-meta");
        textBox.getChildren().addAll(title, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(tag, textBox);
        return row;
    }

    @FXML
    public void recordDonation() {
        LocalDate donationDate = datePicker.getValue();
        int quantity = quantitySpinner.getValue();
        if (donationDate == null) {
            info("Record donation", "Select the donation date.");
            return;
        }
        if (donationDate.isAfter(LocalDate.now())) {
            info("Record donation", "Donation date cannot be in the future.");
            return;
        }
        if (quantity <= 0) {
            info("Record donation", "Donation quantity must be greater than zero.");
            return;
        }

        repository.save(new Donation(0, donor.getId(), donationDate, donor.getBloodType(), quantity));
        donor.setLastDonationDate(donationDate);
        userRepository.updateDonor(donor);
        activityLogRepository.log(donor.getId(), "DONATION_RECORDED",
                quantity + " unit(s) donated on " + donationDate + ".");
        refresh();
    }

    @FXML
    public void exportCsv() {
        if (currentDonations.isEmpty()) {
            info("Export CSV", "There are no donations to export yet.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Donation History");
        chooser.setInitialFileName("lifelink_donation_history.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        var file = chooser.showSaveDialog(donationListBox.getScene().getWindow());
        if (file == null) return;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Date,Blood Type,Quantity\n");
            for (Donation d : currentDonations) {
                writer.write(d.getDonationDate() + "," + d.getBloodType().getLabel() + "," + d.getQuantity() + "\n");
            }
            info("Export CSV", "Donation history exported to " + file.getName() + ".");
        } catch (IOException e) {
            info("Export CSV", "Unable to export file: " + e.getMessage());
        }
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
