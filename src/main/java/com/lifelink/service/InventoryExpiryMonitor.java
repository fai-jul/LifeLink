package com.lifelink.service;

import com.lifelink.model.BloodUnit;
import com.lifelink.model.NotificationType;
import javafx.application.Platform;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InventoryExpiryMonitor implements AutoCloseable {

    private final InventoryService inventoryService;
    private final Runnable refreshCallback;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "lifelink-expiry-monitor");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Integer> warnedUnitIds = new HashSet<>();

    public InventoryExpiryMonitor(InventoryService inventoryService, Runnable refreshCallback) {
        this.inventoryService = inventoryService;
        this.refreshCallback = refreshCallback;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::sweep, 0, 1, TimeUnit.MINUTES);
    }

    private void sweep() {
        boolean changed = false;
        for (BloodUnit unit : inventoryService.repository().findExpiredCandidates()) {
            inventoryService.repository().markExpired(unit.getId());
            inventoryService.notifications().create(unit.getBloodBankId(),
                    unit.getQuantity() + " " + unit.getBloodType().getLabel()
                            + " unit(s) expired and were removed from available inventory.",
                    NotificationType.EXPIRY);
            inventoryService.activityLogs().log(unit.getBloodBankId(), "INVENTORY_EXPIRED",
                    "Inventory item #" + unit.getId() + " expired.");
            inventoryService.notifyInventoryState(unit.getBloodBankId());
            changed = true;
        }
        for (BloodUnit unit : inventoryService.repository().findExpiringSoon(7)) {
            if (warnedUnitIds.add(unit.getId())) {
                inventoryService.notifications().create(unit.getBloodBankId(),
                        unit.getQuantity() + " " + unit.getBloodType().getLabel()
                                + " unit(s) expire by " + unit.getExpiryDate() + ".",
                        NotificationType.EXPIRY);
                inventoryService.activityLogs().log(unit.getBloodBankId(), "EXPIRY_WARNING",
                        "Inventory item #" + unit.getId() + " is expiring soon.");
            }
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
