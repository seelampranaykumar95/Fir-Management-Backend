package com.fir.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fir.dto.ApiErrorResponse;
import com.fir.config.security.JwtAuthenticationFilter;
import com.fir.service.AuditLogService;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper, AuditLogService auditLogService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/auth/login", "/auth/register").permitAll()
                    .requestMatchers("/stations/**", "/reports/**", "/audit-logs/**").hasRole("ADMIN")
                    .requestMatchers("/assignments/**").hasAnyRole("ADMIN", "OFFICER")
                    .requestMatchers("/officers/**").hasRole("OFFICER")
                    .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                        log.warn(
                                "Authentication failure requestId={} method={} path={} message={}",
                                MDC.get(RequestCorrelationFilter.REQUEST_ID_KEY),
                                request.getMethod(),
                                request.getRequestURI(),
                                authException.getMessage());
                        auditLogService.recordAction(
                                null,
                                "AUTHENTICATION_FAILED",
                                "SECURITY",
                                null,
                                "Authentication failed for path " + request.getRequestURI() + ": " + authException.getMessage());
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(objectMapper.writeValueAsString(
                                buildSecurityError(HttpStatus.UNAUTHORIZED, authException.getMessage(), request.getRequestURI())));
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                        com.fir.model.User actor = null;
                        String principal = authentication == null ? "anonymous" : authentication.getName();
                        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                            // actor will be resolved in controllers/services when needed; here we log email only via details
                        }
                        log.warn(
                                "Access denied requestId={} method={} path={} principal={} message={}",
                                MDC.get(RequestCorrelationFilter.REQUEST_ID_KEY),
                                request.getMethod(),
                                request.getRequestURI(),
                                principal,
                                accessDeniedException.getMessage());
                        auditLogService.recordAction(
                                actor,
                                "ACCESS_DENIED",
                                "SECURITY",
                                null,
                                "Access denied for path " + request.getRequestURI() + ": " + accessDeniedException.getMessage());
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(objectMapper.writeValueAsString(
                                buildSecurityError(HttpStatus.FORBIDDEN, accessDeniedException.getMessage(), request.getRequestURI())));
                    }))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    private ApiErrorResponse buildSecurityError(HttpStatus status, String message, String path) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(message);
        response.setPath(path);
        return response;
    }
}


