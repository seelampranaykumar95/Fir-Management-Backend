package com.fir.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.fir.model.User;
import com.fir.model.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByRole(UserRole role);

    Optional<User> findByEmail(String email);

    Optional<User> findByAadhaarNumber(String aadhaarNumber);

    boolean existsByEmail(String email);

    boolean existsByAadhaarNumber(String aadhaarNumber);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByAadhaarNumberAndIdNot(String aadhaarNumber, Long id);
}

