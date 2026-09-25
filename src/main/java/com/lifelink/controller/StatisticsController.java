package com.lifelink.controller;

import com.lifelink.SceneManager;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.BloodUnitRepository;
import com.lifelink.db.DonationRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.BloodType;
import com.lifelink.model.Donor;
import com.lifelink.model.User;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.util.List;

public class StatisticsController {
    @FXML private Label personalTitle;
    @FXML private Label donationCount;
    @FXML private Label unitsGiven;
    @FXML private Label personalType;
    @FXML private Label personalStatus;
    @FXML private Label donorCount;
    @FXML private Label availableDonorCount;
    @FXML private Label requestCount;
    @FXML private Label bankCount;
    @FXML private Label inventoryCount;
    @FXML private PieChart typeChart;
    @FXML private BarChart<String, Number> networkChart;

    private final UserRepository userRepository = new UserRepository();
    private final DonationRepository donationRepository = new DonationRepository();
    private final BloodRequestRepository requestRepository = new BloodRequestRepository();
    private final BloodUnitRepository bloodUnitRepository = new BloodUnitRepository();

    @FXML
    public void initialize() {
        User user = SceneManager.getCurrentUser();
        if (user instanceof Donor donor) {
            var donations = donationRepository.findForDonor(donor.getId());
            personalTitle.setText(donor.getName() + "'s donation story");
            donationCount.setText(String.valueOf(donations.size()));
            unitsGiven.setText(String.valueOf(donations.stream().mapToInt(donation -> donation.getQuantity()).sum()));
            personalType.setText(donor.getBloodType() == null ? "-" : donor.getBloodType().getLabel());
            personalStatus.setText(donations.isEmpty()
                    ? "No completed donations recorded yet"
                    : donor.isAvailable() ? "Available to help" : "Currently unavailable");
        } else {
            personalTitle.setText("Your donation story");
            donationCount.setText("-");
            unitsGiven.setText("-");
            personalType.setText("-");
            personalStatus.setText("Log in as a donor to see personal donation history");
        }

        List<Donor> allDonors = userRepository.findAllDonors(false);
        List<Donor> availableDonors = userRepository.findAllDonors(true);
        donorCount.setText(String.valueOf(allDonors.size()));
        availableDonorCount.setText(String.valueOf(availableDonors.size()));
        requestCount.setText(String.valueOf(requestRepository.findActive().size()));
        bankCount.setText(String.valueOf(userRepository.findAllBloodBanks().size()));
        int inventoryUnits = 0;
        for (var bank : userRepository.findAllBloodBanks()) {
            inventoryUnits += bloodUnitRepository.totalAvailable(bank.getId());
        }
        inventoryCount.setText(String.valueOf(inventoryUnits));
        loadCharts(allDonors);
    }

    private void loadCharts(List<Donor> donors) {
        typeChart.getData().clear();
        for (BloodType type : BloodType.values()) {
            long count = donors.stream().filter(donor -> donor.getBloodType() == type).count();
            if (count > 0) typeChart.getData().add(new PieChart.Data(type.getLabel(), count));
        }
        typeChart.setTitle(typeChart.getData().isEmpty() ? "No donor data available" : null);
        networkChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Locations");
        series.getData().add(new XYChart.Data<>("Donors", donors.size()));
        series.getData().add(new XYChart.Data<>("Available", (int) donors.stream().filter(Donor::isAvailable).count()));
        series.getData().add(new XYChart.Data<>("Requests", requestRepository.findActive().size()));
        networkChart.getData().add(series);
    }

    @FXML
    public void goBack() {
        SceneManager.goToDashboard();
    }

    @FXML
    public void openMap() {
        SceneManager.switchTo("map.fxml", "Nearby Network");
    }
}
