package com.carpooling.controller;

import com.carpooling.model.Dispute;
import com.carpooling.service.CustomUserDetails;
import com.carpooling.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/disputes")
public class DisputeController {
    @Autowired
    private DisputeService disputeService;

    @PostMapping("/raise")
    public Dispute raise(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @RequestBody Map<String, Object> body) {
        Long rideId = Long.parseLong(String.valueOf(body.get("rideId")));
        Long bookingId = body.get("bookingId") == null ? null : Long.parseLong(String.valueOf(body.get("bookingId")));
        String reason = body.get("reason") == null ? "" : String.valueOf(body.get("reason"));
        return disputeService.raiseDispute(userDetails.getUser().getId(), rideId, bookingId, reason);
    }

    @GetMapping("/my")
    public List<Dispute> my(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return disputeService.myDisputes(userDetails.getUser().getId());
    }
}
