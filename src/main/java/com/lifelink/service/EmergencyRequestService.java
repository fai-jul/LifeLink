package com.lifelink.service;

import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.BloodRequestRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.*;

import java.time.LocalDateTime;
import java.util.List;

public class EmergencyRequestService {

    private final BloodRequestRepository repository = new BloodRequestRepository();
    private final MatchingService matchingService = new MatchingService();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();
    private final UserRepository userRepository = new UserRepository();

    public BloodRequest createAndMatch(int recipientId, BloodType type, int quantity, String location,
                                       double latitude, double longitude, RequestPriority priority) {
        if (type == null) throw new IllegalArgumentException("Select a blood type.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        if (priority == null) throw new IllegalArgumentException("Select request priority.");
        BloodRequest request = new BloodRequest(0, recipientId, type, quantity, location,
                latitude, longitude, priority, RequestStatus.PENDING, LocalDateTime.now());
        request = repository.create(request);
        activityLogRepository.log(recipientId, "REQUEST_CREATED",
                "Emergency blood request #" + request.getId() + " created.");
        for (int bloodBankId : userRepository.findAllBloodBankIds()) {
            notificationRepository.create(bloodBankId,
                    "New " + priority + " emergency blood request #" + request.getId()
                            + " for " + type.getLabel() + ".",
                    NotificationType.EMERGENCY);
        }
        repository.updateStatus(request.getId(), RequestStatus.SEARCHING,
                "Searching compatible, eligible donors.");
        List<MatchingService.MatchCandidate> matches = matchingService.findAndNotify(request);
        RequestStatus finalStatus = matches.isEmpty() ? RequestStatus.NO_MATCH : RequestStatus.MATCHED;
        repository.updateStatus(request.getId(), finalStatus,
                matches.isEmpty() ? "No compatible eligible donors found." :
                        matches.size() + " compatible donor(s) notified.");
        notificationRepository.create(recipientId,
                matches.isEmpty() ? "No compatible donors were found for request #" + request.getId() + "."
                        : matches.size() + " donor(s) were notified for request #" + request.getId() + ".",
                matches.isEmpty() ? NotificationType.EMERGENCY : NotificationType.MATCH);
        activityLogRepository.log(recipientId, "REQUEST_STATUS",
                "Emergency request #" + request.getId() + " is now " + finalStatus + ".");
        request.setStatus(finalStatus);
        return request;
    }

    public List<BloodRequest> requestsFor(int recipientId) {
        return repository.findForRecipient(recipientId);
    }

    public List<RequestStatusEvent> timeline(int requestId) {
        return repository.historyFor(requestId);
    }

    public void cancel(BloodRequest request) {
        repository.updateStatus(request.getId(), RequestStatus.CANCELLED, "Cancelled by recipient.");
    }

    public BloodRequestRepository repository() {
        return repository;
    }
}
