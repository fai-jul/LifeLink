package com.lifelink.service;

import com.lifelink.model.Donor;

import java.time.LocalDate;

/**
 * Encapsulates the (simplified, non-medical) donor eligibility rules
 * described in the spec: minimum donation gap, availability flag, and
 * basic age/weight sanity bounds already enforced at registration.
 */
public class EligibilityService {

    public static final int MIN_DONATION_GAP_DAYS = 90;

    public boolean isEligible(Donor donor) {
        if (!donor.isAvailable()) return false;
        LocalDate last = donor.getLastDonationDate();
        if (last == null) return true; // never donated -> eligible
        return LocalDate.now().isAfter(nextEligibleDate(donor)) || LocalDate.now().isEqual(nextEligibleDate(donor));
    }

    public LocalDate nextEligibleDate(Donor donor) {
        LocalDate last = donor.getLastDonationDate();
        if (last == null) return LocalDate.now();
        return last.plusDays(MIN_DONATION_GAP_DAYS);
    }

    public long daysUntilEligible(Donor donor) {
        if (isEligible(donor)) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextEligibleDate(donor));
    }
}
