package com.carpooling.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class DistanceService {
    private static final double DEFAULT_DISTANCE_KM = 12.0;

    @Value("${maps.distance.api.key:}")
    private String mapsApiKey;

    @SuppressWarnings("unchecked")
    public double calculateDistanceKm(String source, String destination) {
        Double apiDistance = fetchDistanceFromGoogle(source, destination);
        if (apiDistance != null && apiDistance > 0) {
            return round(apiDistance);
        }
        Double heuristics = estimateByKnownCities(source, destination);
        if (heuristics != null) {
            return round(heuristics);
        }
        return DEFAULT_DISTANCE_KM;
    }

    private Double fetchDistanceFromGoogle(String source, String destination) {
        if (mapsApiKey == null || mapsApiKey.isBlank()) {
            return null;
        }
        try {
            String url = "https://maps.googleapis.com/maps/api/distancematrix/json?units=metric&origins="
                    + source.replace(" ", "%20")
                    + "&destinations="
                    + destination.replace(" ", "%20")
                    + "&key=" + mapsApiKey;
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> json = restTemplate.getForObject(url, Map.class);
            if (json == null) {
                return null;
            }
            Object rowsObj = json.get("rows");
            if (!(rowsObj instanceof java.util.List<?> rows) || rows.isEmpty()) {
                return null;
            }
            Object firstRow = rows.get(0);
            if (!(firstRow instanceof Map<?, ?> rowMap)) {
                return null;
            }
            Object elementsObj = rowMap.get("elements");
            if (!(elementsObj instanceof java.util.List<?> elements) || elements.isEmpty()) {
                return null;
            }
            Object firstElement = elements.get(0);
            if (!(firstElement instanceof Map<?, ?> elementMap)) {
                return null;
            }
            Object distanceObj = elementMap.get("distance");
            if (!(distanceObj instanceof Map<?, ?> distanceMap)) {
                return null;
            }
            Object metersObj = distanceMap.get("value");
            if (!(metersObj instanceof Number meters)) {
                return null;
            }
            return meters.doubleValue() / 1000.0;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double estimateByKnownCities(String source, String destination) {
        Map<String, double[]> coords = new HashMap<>();
        coords.put("hyderabad", new double[]{17.3850, 78.4867});
        coords.put("secunderabad", new double[]{17.4399, 78.4983});
        coords.put("gachibowli", new double[]{17.4401, 78.3489});
        coords.put("hitech city", new double[]{17.4474, 78.3762});
        coords.put("kukatpally", new double[]{17.4948, 78.3996});
        coords.put("madhapur", new double[]{17.4483, 78.3915});
        coords.put("begumpet", new double[]{17.4435, 78.4691});
        coords.put("uppal", new double[]{17.4058, 78.5591});

        double[] s = lookupCoords(source, coords);
        double[] d = lookupCoords(destination, coords);
        if (s == null || d == null) {
            return null;
        }
        return haversineKm(s[0], s[1], d[0], d[1]) * 1.25;
    }

    private double[] lookupCoords(String place, Map<String, double[]> coords) {
        if (place == null) {
            return null;
        }
        String normalized = place.toLowerCase(Locale.ROOT).trim();
        for (Map.Entry<String, double[]> entry : coords.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}