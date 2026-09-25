package com.lifelink.service;

import com.lifelink.db.ActivityLogRepository;
import com.lifelink.db.NotificationRepository;
import com.lifelink.db.UserRepository;
import com.lifelink.model.*;
import com.lifelink.util.DistanceUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class MatchingService {

    public record MatchCandidate(Donor donor, double distanceKm, double score) {
    }

    private final UserRepository userRepository = new UserRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final ActivityLogRepository activityLogRepository = new ActivityLogRepository();
    private final CompatibilityService compatibilityService = new CompatibilityService();
    private final EligibilityService eligibilityService = new EligibilityService();

    public List<MatchCandidate> findAndNotify(BloodRequest request) {
        List<Donor> donors = userRepository.findAllDonors(true);
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.max(1, Math.min(8, donors.size())));
        try {
            List<Future<MatchCandidate>> futures = new ArrayList<>();
            for (Donor donor : donors) {
                futures.add(executor.submit(() -> score(request, donor)));
            }
            List<MatchCandidate> matches = new ArrayList<>();
            for (Future<MatchCandidate> future : futures) {
                try {
                    MatchCandidate candidate = future.get();
                    if (candidate != null) matches.add(candidate);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException ignored) {
                    // One malformed donor must not prevent other donors being searched.
                }
            }
            matches.sort(Comparator.comparingDouble(MatchCandidate::score).reversed());
            List<MatchCandidate> notified = matches.stream().limit(10).toList();
            notifyDonors(request, notified);
            return notified;
        } finally {
            executor.shutdown();
        }
    }

    private MatchCandidate score(BloodRequest request, Donor donor) {
        if (!compatibilityService.isCompatible(donor.getBloodType(), request.getBloodType())
                || !eligibilityService.isEligible(donor)) {
            return null;
        }
        double distance = DistanceUtil.haversineKm(
                donor.getLatitude(), donor.getLongitude(),
                request.getLatitude(), request.getLongitude());
        double score = 60 + 25 + Math.max(0, 15 - Math.min(15, distance));
        return new MatchCandidate(donor, distance, score);
    }

    private void notifyDonors(BloodRequest request, List<MatchCandidate> matches) {
        for (MatchCandidate match : matches) {
            notificationRepository.create(match.donor().getId(),
                    "Compatible emergency request #" + request.getId() + " found "
                            + DistanceUtil.format(match.distanceKm()) + " away.",
                    NotificationType.MATCH);
            activityLogRepository.log(match.donor().getId(), "MATCH_NOTIFICATION",
                    "Notified about emergency request #" + request.getId() + ".");
        }
    }
}
