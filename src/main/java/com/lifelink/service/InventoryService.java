package com.lifelink.service;

import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.BloodUnitRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.model.*;

import java.time.LocalDate;
import java.util.List;

public class InventoryService {

    public static final int LOW_STOCK_THRESHOLD = 5;

    private final BloodUnitRepository repository = new BloodUnitRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    public BloodUnit add(int bankId, BloodType type, int quantity, LocalDate collectionDate,
                         LocalDate expiryDate) {
        return add(bankId, type, BloodComponent.WHOLE_BLOOD, quantity, collectionDate, expiryDate, "Unassigned");
    }

    public BloodUnit add(int bankId, BloodType type, BloodComponent component, int quantity,
                         LocalDate collectionDate, LocalDate expiryDate, String storageLocation) {
        if (expiryDate == null && collectionDate != null) {
            expiryDate = BloodUnit.defaultExpiryFrom(collectionDate);
        }
        validate(type, component, quantity, collectionDate, expiryDate);
        BloodUnit unit = new BloodUnit(0, bankId, type, component, quantity, collectionDate, expiryDate,
                BloodUnitStatus.AVAILABLE, storageLocation);
        BloodUnit saved = repository.save(unit);
        activityLogRepository.log(bankId, "INVENTORY_ADD",
                quantity + " " + type.getLabel() + " unit(s) added.");
        notifyInventoryState(bankId);
        return saved;
    }

    public void update(BloodUnit unit) {
        validate(unit.getBloodType(), unit.getComponent(), unit.getQuantity(), unit.getCollectionDate(), unit.getExpiryDate());
        repository.update(unit);
        activityLogRepository.log(unit.getBloodBankId(), "INVENTORY_UPDATE",
                "Inventory item #" + unit.getId() + " updated.");
        notifyInventoryState(unit.getBloodBankId());
    }

    public void remove(BloodUnit unit) {
        repository.delete(unit.getId(), unit.getBloodBankId());
        activityLogRepository.log(unit.getBloodBankId(), "INVENTORY_REMOVE",
                "Inventory item #" + unit.getId() + " removed.");
        notifyInventoryState(unit.getBloodBankId());
    }

    public List<BloodUnit> search(int bankId, String search, BloodType type, BloodUnitStatus status) {
        return repository.findForBank(bankId, search, type, status);
    }

    public int totalAvailable(int bankId) {
        return repository.totalAvailable(bankId);
    }

    public void notifyInventoryState(int bankId) {
        int total = repository.totalAvailable(bankId);
        if (total < LOW_STOCK_THRESHOLD) {
            notificationRepository.create(bankId,
                    "Low stock alert: only " + total + " available blood unit(s) remain.",
                    NotificationType.INVENTORY);
        }
    }

    BloodUnitRepository repository() {
        return repository;
    }

    NotificationRepository notifications() {
        return notificationRepository;
    }

    ActivityLogRepository activityLogs() {
        return activityLogRepository;
    }

    private void validate(BloodType type, BloodComponent component, int quantity,
                          LocalDate collectionDate, LocalDate expiryDate) {
        if (type == null) throw new IllegalArgumentException("Select a blood type.");
        if (component == null) throw new IllegalArgumentException("Select a blood component.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        if (collectionDate == null || expiryDate == null) {
            throw new IllegalArgumentException("Collection and expiry dates are required.");
        }
        if (expiryDate.isBefore(collectionDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before collection date.");
        }
    }
}
