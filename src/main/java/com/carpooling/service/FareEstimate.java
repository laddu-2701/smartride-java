package com.carpooling.service;

public class FareEstimate {
    private double distanceKm;
    private double baseFare;
    private double ratePerKm;
    private double fareBeforeSplit;
    private int seats;
    private int sharedByPassengers;
    private double totalFare;

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }
    public double getRatePerKm() { return ratePerKm; }
    public void setRatePerKm(double ratePerKm) { this.ratePerKm = ratePerKm; }
    public double getFareBeforeSplit() { return fareBeforeSplit; }
    public void setFareBeforeSplit(double fareBeforeSplit) { this.fareBeforeSplit = fareBeforeSplit; }
    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }
    public int getSharedByPassengers() { return sharedByPassengers; }
    public void setSharedByPassengers(int sharedByPassengers) { this.sharedByPassengers = sharedByPassengers; }
    public double getTotalFare() { return totalFare; }
    public void setTotalFare(double totalFare) { this.totalFare = totalFare; }
}