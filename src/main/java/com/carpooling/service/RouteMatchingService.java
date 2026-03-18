package com.carpooling.service;

import com.carpooling.model.Ride;
import com.carpooling.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RouteMatchingService {
    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private DistanceService distanceService;

    public List<RideMatchResult> findMatches(String passengerSource, String passengerDestination, LocalDate date) {
        List<Ride> candidates = rideRepository.findByDateAndStatus(date, "SCHEDULED");
        List<RideMatchResult> result = new ArrayList<>();

        for (Ride ride : candidates) {
            double score = scoreRide(ride, passengerSource, passengerDestination);
            if (score >= 0.35) {
                RideMatchResult match = new RideMatchResult();
                match.setRide(ride);
                match.setMatchType(score >= 0.85 ? "DIRECT" : "PARTIAL");
                match.setMatchScore(round(score));
                result.add(match);
            }
        }

        result.sort(Comparator.comparingDouble(RideMatchResult::getMatchScore).reversed());
        return result;
    }

    private double scoreRide(Ride ride, String passengerSource, String passengerDestination) {
        String rideSource = normalize(ride.getSource());
        String rideDestination = normalize(ride.getDestination());
        String pSource = normalize(passengerSource);
        String pDestination = normalize(passengerDestination);

        if (rideSource.equals(pSource) && rideDestination.equals(pDestination)) {
            return 1.0;
        }

        double sourceSimilarity = textSimilarity(rideSource, pSource);
        double destinationSimilarity = textSimilarity(rideDestination, pDestination);
        double lexical = (sourceSimilarity + destinationSimilarity) / 2.0;

        double rideDistance = distanceService.calculateDistanceKm(ride.getSource(), ride.getDestination());
        double passengerDistance = distanceService.calculateDistanceKm(passengerSource, passengerDestination);
        double distanceScore = 1.0 - Math.min(Math.abs(rideDistance - passengerDistance) / Math.max(rideDistance, 1.0), 1.0);

        return (0.7 * lexical) + (0.3 * distanceScore);
    }

    private double textSimilarity(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.contains(b) || b.contains(a)) {
            return 0.85;
        }
        int commonTokens = 0;
        String[] aTokens = a.split("\\s+");
        String[] bTokens = b.split("\\s+");
        for (String at : aTokens) {
            for (String bt : bTokens) {
                if (at.equals(bt)) {
                    commonTokens++;
                }
            }
        }
        int maxTokens = Math.max(aTokens.length, bTokens.length);
        if (maxTokens == 0) {
            return 0.0;
        }
        return (double) commonTokens / maxTokens;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}