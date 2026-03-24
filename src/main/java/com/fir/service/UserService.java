package com.fir.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.fir.dto.UserDtos.UpdateUserRequest;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User saveUser(User user) {
        normalizeUser(user);
        if (user.getRole() == null) {
            user.setRole(UserRole.CITIZEN);
        }
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (user.getAadhaarNumber() != null && userRepository.existsByAadhaarNumber(user.getAadhaarNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aadhaar number already registered");
        }
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with email: " + email));
    }

    public User getByIdentifier(String identifier) {
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByAadhaarNumber(identifier))
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found for identifier: " + identifier));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .toList();
    }

    public User getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
        return user;
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .filter(User::isActive)
                .toList();
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> {
                    if (!user.isActive()) {
                        throw new IllegalStateException("Inactive users should be filtered at query level");
                    }
                    return user;
                });
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User existingUser = getUserById(id);
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedAadhaar = normalizeAadhaar(request.getAadhaarNumber());

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (userRepository.existsByAadhaarNumberAndIdNot(normalizedAadhaar, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aadhaar number already registered");
        }

        existingUser.setName(request.getName().trim());
        existingUser.setEmail(normalizedEmail);
        existingUser.setAadhaarNumber(normalizedAadhaar);
        existingUser.setRole(request.getRole() == null ? existingUser.getRole() : request.getRole());
        if (StringUtils.hasText(request.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
        if (!user.isActive()) {
            return;
        }
        user.setActive(false);
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    private void normalizeUser(User user) {
        user.setName(user.getName() == null ? null : user.getName().trim());
        user.setEmail(normalizeEmail(user.getEmail()));
        user.setAadhaarNumber(normalizeAadhaar(user.getAadhaarNumber()));
        user.setPassword(user.getPassword() == null ? null : user.getPassword().trim());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAadhaar(String aadhaarNumber) {
        return aadhaarNumber == null ? null : aadhaarNumber.trim();
    }
}

