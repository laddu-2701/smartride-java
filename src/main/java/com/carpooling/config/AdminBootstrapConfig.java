package com.carpooling.config;

import com.carpooling.model.Role;
import com.carpooling.model.User;
import com.carpooling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {
    @Bean
    public CommandLineRunner createDefaultAdmin(UserRepository userRepository,
                                                PasswordEncoder passwordEncoder,
                                                @Value("${app.admin.bootstrap.enabled:true}") boolean bootstrapEnabled,
                                                @Value("${app.admin.email:admin@smartride.local}") String adminEmail,
                                                @Value("${app.admin.password:Admin@123}") String adminPassword) {
        return args -> {
            if (!bootstrapEnabled) {
                return;
            }
            if (userRepository.findFirstByRole(Role.ADMIN).isPresent()) {
                return;
            }

            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail(adminEmail);
            admin.setPhone("0000000000");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setBlocked(false);
            admin.setDriverVerified(true);
            userRepository.save(admin);
        };
    }
}
