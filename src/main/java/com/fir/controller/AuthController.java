package com.fir.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fir.config.RequestCorrelationFilter;
import com.fir.config.security.CustomUserPrincipal;
import com.fir.dto.AuthDtos.AuthResponse;
import com.fir.dto.AuthDtos.CurrentUserResponse;
import com.fir.dto.AuthDtos.LoginRequest;
import com.fir.dto.AuthDtos.RegisterRequest;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.config.security.JwtTokenProvider;
import com.fir.service.AuditLogService;
import com.fir.service.UserService;

import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          UserService userService,
                          AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User(request.getName(), request.getEmail(), request.getPassword(), UserRole.CITIZEN);
        user.setAadhaarNumber(request.getAadhaarNumber());
        User saved = userService.saveUser(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        auditLogService.recordAction(saved, "USER_REGISTERED", "USER", saved.getId(), "Citizen self-registration completed");

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved, jwt));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.getIdentifier();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userService.getByIdentifier(identifier);
        auditLogService.recordAction(user, "USER_LOGGED_IN", "USER", user.getId(), "Successful authentication");
        return ResponseEntity.ok(toResponse(user, jwt));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal CustomUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = userService.getUserById(principal.getId());
        log.info(
                "Resolved authenticated user requestId={} principal={} userId={} role={}",
                MDC.get(RequestCorrelationFilter.REQUEST_ID_KEY),
                principal.getUsername(),
                user.getId(),
                user.getRole());
        return ResponseEntity.ok(toCurrentUserResponse(user));
    }

    private AuthResponse toResponse(User user, String jwt) {
        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setAadhaarNumber(maskAadhaar(user.getAadhaarNumber()));
        response.setRole(user.getRole());
        return response;
    }

    private CurrentUserResponse toCurrentUserResponse(User user) {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setAadhaarNumber(maskAadhaar(user.getAadhaarNumber()));
        response.setRole(user.getRole());
        return response;
    }

    private String maskAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) {
            return null;
        }
        return "XXXXXXXX" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }
}


