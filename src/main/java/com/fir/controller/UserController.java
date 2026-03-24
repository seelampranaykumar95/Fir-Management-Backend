package com.fir.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fir.dto.PageResponse;
import com.fir.dto.UserDtos;
import com.fir.dto.UserDtos.CreateUserRequest;
import com.fir.dto.UserDtos.UpdateUserRequest;
import com.fir.dto.UserDtos.UserResponse;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.service.AuditLogService;
import com.fir.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        User user = new User(request.getName(), request.getEmail(), request.getPassword(), request.getRole());
        user.setAadhaarNumber(request.getAadhaarNumber());
        User saved = userService.saveUser(user);
        auditLogService.recordAction(
                userService.getByEmail(authentication.getName()),
                "USER_CREATED",
                "USER",
                saved.getId(),
                "Created user with role " + saved.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDtos.fromEntity(saved));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                userService.getAllUsers(pageable).map(UserDtos::fromEntity)));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable UserRole role) {
        List<UserResponse> users = userService.getUsersByRole(role).stream()
                .map(UserDtos::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(UserDtos.fromEntity(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        User updated = userService.updateUser(id, request);
        auditLogService.recordAction(
                userService.getByEmail(authentication.getName()),
                "USER_UPDATED",
                "USER",
                updated.getId(),
                "Updated user profile");
        return ResponseEntity.ok(UserDtos.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id);
        auditLogService.recordAction(
                userService.getByEmail(authentication.getName()),
                "USER_DELETED",
                "USER",
                id,
                "Deleted user account");
        return ResponseEntity.noContent().build();
    }
}

