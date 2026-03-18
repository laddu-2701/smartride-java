package com.carpooling.service;

import com.carpooling.model.Ride;

public class RideMatchResult {
    private Ride ride;
    private String matchType;
    private double matchScore;

    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }
    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
}