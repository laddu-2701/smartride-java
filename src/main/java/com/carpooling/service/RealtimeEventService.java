package com.carpooling.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RealtimeEventService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyRideUpdate(Long rideId, String action, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rideId", rideId);
        payload.put("action", action);
        payload.put("message", message);
        messagingTemplate.convertAndSend("/topic/ride-updates", payload);
    }

    public void notifyBookingUpdate(Long rideId, Long bookingId, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rideId", rideId);
        payload.put("bookingId", bookingId);
        payload.put("message", message);
        messagingTemplate.convertAndSend("/topic/booking-updates", payload);
    }
}