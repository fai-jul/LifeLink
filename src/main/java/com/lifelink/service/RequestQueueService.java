package com.lifelink.service;

import com.lifelink.model.BloodRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Orders simultaneous emergency requests with a PriorityQueue so blood
 * banks always triage CRITICAL requests first, then HIGH, then NORMAL --
 * ties broken by whichever request arrived first (FIFO within a tier).
 *
 * This is the "PriorityQueue for simultaneous urgent requests" item from
 * the Phase 4 roadmap. RequestPriority's natural weight ordering
 * (CRITICAL=0, HIGH=1, NORMAL=2) makes the comparator a one-liner.
 */
public class RequestQueueService {

    public List<BloodRequest> orderByPriority(List<BloodRequest> requests) {
        PriorityQueue<BloodRequest> queue = new PriorityQueue<>(
                Comparator.<BloodRequest>comparingInt(r -> r.getUrgency().getWeight())
                        .thenComparing(BloodRequest::getCreatedAt));
        queue.addAll(requests);
        List<BloodRequest> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }
}
