package com.lifelink.service;

import com.lifelink.model.BloodType;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps BloodType.canDonateTo() so compatibility logic has a single,
 * testable service entry point rather than being scattered through
 * controllers (per the spec's architecture rule).
 */
public class CompatibilityService {

    public boolean isCompatible(BloodType donorType, BloodType recipientType) {
        if (donorType == null || recipientType == null) return false;
        return donorType.canDonateTo(recipientType);
    }

    /** All donor blood types that could supply the given recipient type. */
    public List<BloodType> compatibleDonorTypesFor(BloodType recipientType) {
        List<BloodType> compatible = new ArrayList<>();
        for (BloodType donorType : BloodType.values()) {
            if (donorType.canDonateTo(recipientType)) {
                compatible.add(donorType);
            }
        }
        return compatible;
    }
}
