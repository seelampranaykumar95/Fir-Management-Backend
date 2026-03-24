package com.fir.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail("integration.admin@example.com")) {
            User admin = new User(
                "Integration Admin",
                "integration.admin@example.com",
                passwordEncoder.encode("Password@123"),
                UserRole.ADMIN
            );
            admin.setAadhaarNumber("800000000001");
            userRepository.save(admin);
            System.out.println("Seeded integration admin user.");
        }
        
        if (!userRepository.existsByEmail("integration.officer@example.com")) {
            User officer = new User(
                "Integration Officer",
                "integration.officer@example.com",
                passwordEncoder.encode("Password@123"),
                UserRole.OFFICER
            );
            officer.setAadhaarNumber("800000000002");
            userRepository.save(officer);
            System.out.println("Seeded integration officer user.");
        }
    }
}
