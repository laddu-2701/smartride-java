package com.carpooling.service;

import com.carpooling.model.Ride;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FareService {
    @Autowired
    private DistanceService distanceService;

    @Value("${fare.base:2.50}")
    private double baseFare;

    @Value("${fare.ratePerKm:0.90}")
    private double ratePerKm;

    public FareEstimate calculateFare(Ride ride,
                                      String passengerSource,
                                      String passengerDestination,
                                      int seats,
                                      int passengersSharing) {
        FareEstimate estimate = new FareEstimate();
        double distanceKm = distanceService.calculateDistanceKm(passengerSource, passengerDestination);
        double fareBeforeSplit = baseFare + (ratePerKm * distanceKm);
        int sharingCount = Math.max(passengersSharing, 1);
        double splitFarePerPassenger = fareBeforeSplit / sharingCount;
        double total = splitFarePerPassenger * seats;

        estimate.setDistanceKm(distanceKm);
        estimate.setBaseFare(baseFare);
        estimate.setRatePerKm(ratePerKm);
        estimate.setFareBeforeSplit(round(fareBeforeSplit));
        estimate.setSeats(seats);
        estimate.setSharedByPassengers(sharingCount);
        estimate.setTotalFare(round(total));

        if (ride.getRouteDistanceKm() <= 0) {
            ride.setRouteDistanceKm(distanceService.calculateDistanceKm(ride.getSource(), ride.getDestination()));
        }
        return estimate;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}