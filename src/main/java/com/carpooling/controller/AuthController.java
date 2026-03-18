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
import com.carpooling.service.EmailNotificationService;
import org.springframework.dao.DataIntegrityViolationException;

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

    @Autowired
    private EmailNotificationService emailNotificationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> requestMap) {
        // basic validation and trimming
        String name = requestMap.get("name") != null ? requestMap.get("name").trim() : "";
        String email = requestMap.get("email") != null ? requestMap.get("email").trim() : "";
        String phone = requestMap.get("phone") != null ? requestMap.get("phone").trim() : "";
        String rawPassword = requestMap.get("password");
        String roleStr = requestMap.get("role");

        if (name.isBlank() || rawPassword == null || rawPassword.isBlank()
                || (email.isBlank() && phone.isBlank()) || roleStr == null || roleStr.isBlank()) {
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
            String vehicleModel = requestMap.get("vehicleModel") != null ? requestMap.get("vehicleModel").trim() : "";
            String licensePlate = requestMap.get("licensePlate") != null ? requestMap.get("licensePlate").trim() : "";
            String capacity = requestMap.get("capacity") != null ? requestMap.get("capacity").trim() : "";
            if (vehicleModel.isBlank() || licensePlate.isBlank() || capacity.isBlank()) {
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
        boolean emailSent = emailNotificationService.sendRegistrationEmail(user);

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "emailNotificationSent", emailSent,
                "email", email
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.badRequest().body("Registration failed: duplicate or invalid data");
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

    @PostMapping("/register-driver")
    public ResponseEntity<?> registerAsDriver(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> requestMap) {
        if (userDetails == null || userDetails.getUser() == null || userDetails.getUser().getId() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        if (user.getRole() == Role.DRIVER) {
            return ResponseEntity.badRequest().body("You are already registered as a driver");
        }

        String vehicleModel = requestMap.get("vehicleModel") != null ? requestMap.get("vehicleModel").trim() : "";
        String licensePlate = requestMap.get("licensePlate") != null ? requestMap.get("licensePlate").trim() : "";
        String capacityRaw = requestMap.get("capacity") != null ? requestMap.get("capacity").trim() : "";

        if (vehicleModel.isEmpty() || licensePlate.isEmpty() || capacityRaw.isEmpty()) {
            return ResponseEntity.badRequest().body("Vehicle model, license plate and capacity are required");
        }

        int capacity;
        try {
            capacity = Integer.parseInt(capacityRaw);
        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body("Invalid capacity value");
        }

        if (capacity <= 0) {
            return ResponseEntity.badRequest().body("Capacity must be greater than 0");
        }

        user.setRole(Role.DRIVER);
        user.setVehicleModel(vehicleModel);
        user.setLicensePlate(licensePlate);
        user.setCapacity(capacity);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "You are now registered as a driver",
                "role", user.getRole().name()
        ));
    }

    @GetMapping("/test")
    public String test() {
        return "Server is running!";
    }
}
