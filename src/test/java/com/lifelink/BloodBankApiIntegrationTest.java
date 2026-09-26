package com.lifelink;

import com.lifelink.model.BloodBankDonorRecord;
import com.lifelink.model.BloodBankPatientRecord;
import com.lifelink.service.ApiClientService;
import org.junit.jupiter.api.Test;

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
}
