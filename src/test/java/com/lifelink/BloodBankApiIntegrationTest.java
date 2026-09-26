package com.lifelink;

import com.lifelink.model.BloodBankDonorRecord;
import com.lifelink.model.BloodBankPatientRecord;
import com.lifelink.service.ApiClientService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BloodBankApiIntegrationTest {

    @Test
    void apiServiceSupportsJsonPayloadBinding() {
        ApiClientService service = new ApiClientService();
        BloodBankDonorRecord donor = new BloodBankDonorRecord(
                1, "Rahim Uddin", "O+", "Dhaka", "01810000001", LocalDate.now(), "AVAILABLE");
        BloodBankPatientRecord patient = new BloodBankPatientRecord(
                2, "Karim Ahmed", "A+", "Chittagong", "01810000002", LocalDate.now(), "RECEIVING");

        assertNotNull(service);
        assertEquals("Rahim Uddin", donor.getName());
        assertEquals("Karim Ahmed", patient.getName());
        assertEquals("AVAILABLE", donor.getStatus());
        assertEquals("RECEIVING", patient.getStatus());
    }

    @Test
    void recordModelsRepresentBloodBankTrackedPeople() {
        BloodBankDonorRecord donor = new BloodBankDonorRecord(
                10, "Nasima Akter", "B+", "Sylhet", "01710000010", LocalDate.now(), "AVAILABLE");
        BloodBankPatientRecord patient = new BloodBankPatientRecord(
                20, "Jahid Hasan", "AB+", "Khulna", "01910000020", LocalDate.now(), "IN_TREATMENT");

        assertEquals("Nasima Akter", donor.getName());
        assertEquals("AVAILABLE", donor.getStatus());
        assertEquals("Jahid Hasan", patient.getName());
        assertEquals("IN_TREATMENT", patient.getStatus());
    }

    @Test
    void apiServiceParsesWrappedJsonPayloads() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/donors", exchange -> {
            byte[] response = (
                    "{\"data\":[{\"id\":1,\"name\":\"Rahim Uddin\",\"blood_type\":\"O+\",\"location\":\"Dhaka\",\"phone\":\"01810000001\",\"last_donation_date\":\"2026-09-10\",\"status\":\"AVAILABLE\"}] }"
            ).getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/patients", exchange -> {
            byte[] response = (
                    "{\"data\":[{\"id\":2,\"name\":\"Karim Ahmed\",\"blood_type\":\"A+\",\"location\":\"Chittagong\",\"phone\":\"01810000002\",\"case_date\":\"2026-09-11\",\"status\":\"RECEIVING\"}] }"
            ).getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            ApiClientService service = new ApiClientService();
            List<BloodBankDonorRecord> donors = service.fetchDonors("http://localhost:" + server.getAddress().getPort());
            List<BloodBankPatientRecord> patients = service.fetchPatients("http://localhost:" + server.getAddress().getPort());

            assertEquals(1, donors.size());
            assertEquals("Rahim Uddin", donors.get(0).getName());
            assertEquals("O+", donors.get(0).getBloodType());
            assertEquals("Dhaka", donors.get(0).getLocation());
            assertEquals(LocalDate.of(2026, 9, 10), donors.get(0).getLastDonationDate());

            assertEquals(1, patients.size());
            assertEquals("Karim Ahmed", patients.get(0).getName());
            assertEquals("A+", patients.get(0).getBloodType());
            assertEquals("Chittagong", patients.get(0).getLocation());
            assertEquals(LocalDate.of(2026, 9, 11), patients.get(0).getCaseDate());
        } finally {
            server.stop(0);
        }
    }
}
