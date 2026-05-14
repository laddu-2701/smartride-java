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
            // Skip rides that are already full so that only rides
            // with available seats can be joined by additional passengers.
            if (ride.getAvailableSeats() <= 0) {
                continue;
            }
            MatchComputation computation = scoreRide(ride, passengerSource, passengerDestination);
            if (computation.score >= 0.5) {
                RideMatchResult match = new RideMatchResult();
                match.setRide(ride);
                match.setMatchType(computation.matchType);
                match.setMatchScore(round(computation.score));
                result.add(match);
            }
        }

        result.sort(Comparator.comparingDouble(RideMatchResult::getMatchScore).reversed());
        return result;
    }

    private MatchComputation scoreRide(Ride ride, String passengerSource, String passengerDestination) {
        String rideSource = normalize(ride.getSource());
        String rideDestination = normalize(ride.getDestination());
        String pSource = normalize(passengerSource);
        String pDestination = normalize(passengerDestination);

        MatchComputation computation = new MatchComputation();
        if (rideSource.equals(pSource) && rideDestination.equals(pDestination)) {
            computation.matchType = "DIRECT";
            computation.score = 1.0;
            return computation;
        }

        double rideDistance = Math.max(distanceService.calculateDistanceKm(ride.getSource(), ride.getDestination()), 1.0);
        double toPickup = distanceService.calculateDistanceKm(ride.getSource(), passengerSource);
        double passengerTrip = distanceService.calculateDistanceKm(passengerSource, passengerDestination);
        double toDrop = distanceService.calculateDistanceKm(passengerDestination, ride.getDestination());
        double detourDistance = toPickup + passengerTrip + toDrop;
        double detourRatio = detourDistance / rideDistance;

        double sourceSimilarity = textSimilarity(rideSource, pSource);
        double destinationSimilarity = textSimilarity(rideDestination, pDestination);
        double lexical = (sourceSimilarity + destinationSimilarity) / 2.0;

        boolean sameDestination = rideDestination.equals(pDestination)
            || rideDestination.contains(pDestination)
            || pDestination.contains(rideDestination);

        boolean alongRoute = detourRatio <= 1.35
            && toPickup <= (rideDistance * 0.7)
            && toDrop <= (rideDistance * 0.7)
            && passengerTrip <= (rideDistance * 1.05);

        // Fallback for environments without real distance data (e.g. no Maps API key):
        // when all segments look identical and the passenger is going to the same destination,
        // treat it as a plausible along-route match so mid-route pickups (e.g. Siddipet → Jagtial
        // on a Hyderabad → Jagtial ride) are still surfaced to the user.
        boolean distancesLookDefault = almostEqual(rideDistance, toPickup)
            && almostEqual(rideDistance, passengerTrip)
            && almostEqual(rideDistance, toDrop);
        if (!alongRoute && sameDestination && distancesLookDefault) {
            alongRoute = true;
        }

        if (!alongRoute) {
            // Even if not strictly "along route" by distance heuristics, when the
            // passenger is going to the same destination, treat this as a
            // mid‑route option with a reasonable default score so it shows up.
            if (sameDestination) {
                computation.matchType = "SAME_DEST_MID_ROUTE";
                computation.score = 0.7; // ensure it passes the 0.5 threshold
            } else {
                computation.matchType = "PARTIAL";
                computation.score = 0.0;
            }
            return computation;
        }

        double routeScore;
        if (sameDestination && distancesLookDefault) {
            // Fallback case: treat this as a strong mid-route match even though
            // we only have default distances. Give it a high base score so it
            // clears the 0.5 threshold and surfaces in results.
            routeScore = 0.9;
        } else {
            routeScore = 1.0 - Math.min(Math.max(detourRatio - 1.0, 0.0) / 0.35, 1.0);
        }
        if (sameDestination) {
            // Passenger is heading to the same final destination but may be boarding mid-route.
            // Boost these matches slightly so they are easier to discover when browsing.
            computation.matchType = "SAME_DEST_MID_ROUTE";
            double boostedLexical = 0.5 + (destinationSimilarity * 0.5); // in [0.5, 1.0]
            computation.score = (0.6 * routeScore) + (0.4 * boostedLexical);
        } else {
            computation.matchType = "PARTIAL";
            computation.score = (0.6 * routeScore) + (0.4 * lexical);
        }
        return computation;
    }

    private static class MatchComputation {
        private String matchType;
        private double score;
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

    private boolean almostEqual(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}