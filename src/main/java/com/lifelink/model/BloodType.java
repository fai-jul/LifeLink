package com.lifelink.model;

/**
 * The eight standard blood types plus ABO/Rh compatibility logic.
 *
 * Compatibility here follows standard whole-blood/red-cell donation rules
 * for demonstration purposes. It is a project ranking/matching mechanism,
 * not a medically authoritative clinical decision tool.
 */
public enum BloodType {
    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    private final String label;

    BloodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /** Parses a display label such as "O+" back into the enum constant. */
    public static BloodType fromLabel(String label) {
        for (BloodType type : values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown blood type label: " + label);
    }

    /**
     * Returns true if a donor of this blood type can donate red cells
     * to a recipient of the given blood type.
     */
    public boolean canDonateTo(BloodType recipientType) {
        return switch (this) {
            case O_NEG -> true; // universal donor
            case O_POS -> recipientType == O_POS || recipientType == A_POS
                    || recipientType == B_POS || recipientType == AB_POS;
            case A_NEG -> recipientType == A_NEG || recipientType == A_POS
                    || recipientType == AB_NEG || recipientType == AB_POS;
            case A_POS -> recipientType == A_POS || recipientType == AB_POS;
            case B_NEG -> recipientType == B_NEG || recipientType == B_POS
                    || recipientType == AB_NEG || recipientType == AB_POS;
            case B_POS -> recipientType == B_POS || recipientType == AB_POS;
            case AB_NEG -> recipientType == AB_NEG || recipientType == AB_POS;
            case AB_POS -> recipientType == AB_POS; // universal recipient only accepts AB+
        };
    }

    public boolean isUniversalDonor() {
        return this == O_NEG;
    }

    public boolean isUniversalRecipient() {
        return this == AB_POS;
    }
}
