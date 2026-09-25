package com.lifelink.model;

/**
 * Natural ordering is defined so a PriorityQueue<BloodRequest> will
 * automatically process CRITICAL requests before HIGH before NORMAL.
 */
public enum RequestPriority {
    CRITICAL(0),
    HIGH(1),
    NORMAL(2);

    private final int weight;

    RequestPriority(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
