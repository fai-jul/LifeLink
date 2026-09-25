package com.lifelink.service;

import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.Donor;
import com.lifelink.model.NotificationType;
import javafx.application.Platform;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background monitor (same start/close pattern as InventoryExpiryMonitor)
 * that notifies donors the day they cross back into eligibility after
 * the 90-day donation gap, so they don't have to keep re-checking their
 * dashboard manually. Runs on its own daemon thread; never touches the
 * JavaFX Application Thread except via Platform.runLater().
 */
public class EligibilityReminderMonitor implements AutoCloseable {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final EligibilityService eligibilityService;
    private final Runnable refreshCallback;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "lifelink-eligibility-monitor");
        thread.setDaemon(true);
        return thread;
    });

    // "donorId:date" pairs already notified, so a donor isn't re-notified every sweep.
    private final Set<String> notifiedToday = new HashSet<>();

    public EligibilityReminderMonitor(UserRepository userRepository, NotificationRepository notificationRepository,
                                       ActivityLogRepository activityLogRepository, EligibilityService eligibilityService,
                                       Runnable refreshCallback) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.eligibilityService = eligibilityService;
        this.refreshCallback = refreshCallback;
    }

    public void start() {
        // Eligibility only changes day-to-day, so a 6-hour sweep is plenty.
        executor.scheduleAtFixedRate(this::sweep, 0, 6, TimeUnit.HOURS);
    }

    private void sweep() {
        LocalDate today = LocalDate.now();
        boolean changed = false;
        for (Donor donor : userRepository.findAllDonors(false)) {
            if (donor.getLastDonationDate() == null) continue; // never donated -> already eligible
            if (!eligibilityService.nextEligibleDate(donor).isEqual(today)) continue; // only fire the day it flips
            String key = donor.getId() + ":" + today;
            if (!notifiedToday.add(key)) continue;

            notificationRepository.create(donor.getId(),
                    "You're eligible to donate again! It's been " + EligibilityService.MIN_DONATION_GAP_DAYS
                            + " days since your last donation.", NotificationType.SYSTEM);
            activityLogRepository.log(donor.getId(), "ELIGIBILITY_REMINDER",
                    donor.getName() + " became eligible to donate again.");
            changed = true;
        }
        if (changed && refreshCallback != null) {
            try {
                Platform.runLater(refreshCallback);
            } catch (IllegalStateException ignored) {
                // JavaFX may not be initialized in repository/service tests.
            }
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
