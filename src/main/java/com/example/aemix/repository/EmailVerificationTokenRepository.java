package com.example.aemix.repository;

import com.example.aemix.entity.EmailVerificationToken;
import com.example.aemix.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);

    EmailVerificationToken findEmailVerificationTokenByUser(User user);

    void deleteEmailVerificationTokenByUser(User user);
}
