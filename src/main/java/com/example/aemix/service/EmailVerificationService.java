package com.example.aemix.service;

import com.example.aemix.entity.EmailVerificationToken;
import com.example.aemix.entity.User;
import com.example.aemix.repository.EmailVerificationTokenRepository;
import com.example.aemix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final MailService mailService;

    @Value("${app.verify.base-url}")
    private String baseUrl;

    @Value("${app.verify.token-ttl-minutes}")
    private long ttlMinutes;

    @Transactional
    public void sendVerification(User user) {
        EmailVerificationToken t = tokenRepo.findEmailVerificationTokenByUser(user);
        if (t != null){
            tokenRepo.deleteEmailVerificationTokenByUser(user);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        t = new EmailVerificationToken();
        t.setToken(token);
        t.setUser(user);
        t.setExpiresAt(Instant.now().plus(Duration.ofMinutes(ttlMinutes)));
        tokenRepo.save(t);

        String link = baseUrl + "/api/auth/verify-email?token=" + token;
        mailService.sendVerificationEmail(
                user.getEmail(),
                link,
                ttlMinutes
        );
    }

    @Transactional
    public void verify(String token) {
        EmailVerificationToken t = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (t.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Token expired");
        }

        User user = t.getUser();
        user.setIsVerified(true);

        userRepo.save(user);
        tokenRepo.save(t);
    }
}
