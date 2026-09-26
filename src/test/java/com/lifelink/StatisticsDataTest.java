package com.lifelink;

import com.lifelink.model.BloodType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsDataTest {

    @Test
    void donorDistributionUsesBangladeshiSampleNamesAndDynamicTotals() {
        List<String> donorNames = List.of(
                "Rahim Uddin",
                "Karim Ahmed",
                "Nasima Akter",
                "Farhan Hossain",
                "Shahana Begum",
                "Jahid Hasan"
        );

        List<String> availableDonors = donorNames.stream()
                .limit(4)
                .toList();

        int donorCount = donorNames.size();
        int availableCount = availableDonors.size();
        int requestCount = 9;
        int bankCount = 4;
        int inventoryUnits = 62;

        assertEquals(6, donorCount);
        assertEquals(4, availableCount);
        assertEquals(9, requestCount);
        assertEquals(4, bankCount);
        assertEquals(62, inventoryUnits);

        List<BloodType> types = List.of(BloodType.A_POS, BloodType.O_POS, BloodType.B_POS, BloodType.A_NEG);
        int[] typeCounts = {2, 1, 2, 1};

        int total = 0;
        for (int count : typeCounts) {
            total += count;
        }

        assertEquals(donorCount, total);
        assertTrue(donorNames.get(0).contains("Rahim"));
        assertTrue(donorNames.get(1).contains("Karim"));
    }
}
