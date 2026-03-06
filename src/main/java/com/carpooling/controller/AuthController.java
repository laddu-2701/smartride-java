package com.carpooling.controller;

import com.carpooling.model.Role;
import com.carpooling.model.User;
import com.carpooling.repository.UserRepository;
import com.carpooling.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.carpooling.service.CustomUserDetails;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> requestMap) {
        // basic validation and trimming
        String name = requestMap.get("name");
        String email = requestMap.get("email") != null ? requestMap.get("email").trim() : "";
        String phone = requestMap.get("phone") != null ? requestMap.get("phone").trim() : "";
        String rawPassword = requestMap.get("password");
        String roleStr = requestMap.get("role");

        if (name == null || name.isEmpty() || rawPassword == null || rawPassword.isEmpty()
                || (email.isEmpty() && phone.isEmpty()) || roleStr == null || roleStr.isEmpty()) {
            return ResponseEntity.badRequest().body("Required fields missing");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid role");
        }

        // duplicate check
        if (!email.isEmpty() && userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("User already registered with that email");
        }
        if (!phone.isEmpty() && userRepository.findByPhone(phone).isPresent()) {
            return ResponseEntity.badRequest().body("User already registered with that phone");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        if (role == Role.DRIVER) {
            String vehicleModel = requestMap.get("vehicleModel");
            String licensePlate = requestMap.get("licensePlate");
            String capacity = requestMap.get("capacity");
            if (vehicleModel == null || vehicleModel.isEmpty() || licensePlate == null || licensePlate.isEmpty()
                    || capacity == null || capacity.isEmpty()) {
                return ResponseEntity.badRequest().body("Driver fields required");
            }
            user.setVehicleModel(vehicleModel);
            user.setLicensePlate(licensePlate);
            try {
                user.setCapacity(Integer.parseInt(capacity));
            } catch (NumberFormatException nfe) {
                return ResponseEntity.badRequest().body("Invalid capacity value");
            }
        }

        userRepository.save(user);
        return ResponseEntity.ok("Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username") != null ? body.get("username").trim() : "";
            String password = body.get("password");
            authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> profile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userDetails.getUser());
    }

    @GetMapping("/test")
    public String test() {
        return "Server is running!";
    }
}
