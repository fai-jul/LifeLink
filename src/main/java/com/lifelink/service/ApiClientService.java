package com.lifelink.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifelink.db.BloodBankTrackingRepository;
import com.lifelink.model.BloodBankDonorRecord;
import com.lifelink.model.BloodBankPatientRecord;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ApiClientService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final BloodBankTrackingRepository trackingRepository = new BloodBankTrackingRepository();

    public String getConfiguredBaseUrl() {
        String configured = System.getProperty("lifelink.api.base.url");
        return configured == null || configured.isBlank() ? "http://localhost:8080/api" : configured;
    }

    public List<BloodBankDonorRecord> fetchDonors(String apiHost) {
        String url = normalizeApiBase(apiHost) + "/donors";
        List<BloodBankDonorRecord> donors = fetchList(url, new TypeReference<>() {});
        return donors == null ? new ArrayList<>() : donors;
    }

    public List<BloodBankPatientRecord> fetchPatients(String apiHost) {
        String url = normalizeApiBase(apiHost) + "/patients";
        List<BloodBankPatientRecord> patients = fetchList(url, new TypeReference<>() {});
        return patients == null ? new ArrayList<>() : patients;
    }

    public SyncResult syncBloodBankData(int bloodBankId, String apiHost) {
        List<BloodBankDonorRecord> donors = fetchDonors(apiHost);
        List<BloodBankPatientRecord> patients = fetchPatients(apiHost);

        for (BloodBankDonorRecord donor : donors) {
            donor.setBloodBankId(bloodBankId);
            trackingRepository.saveDonor(donor);
        }

        for (BloodBankPatientRecord patient : patients) {
            patient.setBloodBankId(bloodBankId);
            trackingRepository.savePatient(patient);
        }

        return new SyncResult(donors.size(), patients.size());
    }

    private <T> T fetchList(String url, TypeReference<T> typeReference) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return objectMapper.readValue(response.body(), typeReference);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private String normalizeApiBase(String apiHost) {
        if (apiHost == null || apiHost.isBlank()) {
            return getConfiguredBaseUrl();
        }
        String trimmed = apiHost.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public record SyncResult(int donorsSynced, int patientsSynced) {
    }
}
