package com.example.aemix.services;
import com.example.aemix.config.JwtConfig;
import com.example.aemix.dto.VerifyUserDto;
import com.example.aemix.dto.requests.AuthRequest;
import com.example.aemix.dto.responses.LoginResponse;
import com.example.aemix.entities.User;
import com.example.aemix.entities.enums.Role;
import com.example.aemix.exceptions.BusinessValidationException;
import com.example.aemix.exceptions.ConflictException;
import com.example.aemix.exceptions.EmailSendException;
import com.example.aemix.exceptions.ResourceNotFoundException;
import com.example.aemix.repositories.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtConfig jwtConfig;

    public String register(AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("User with this email already exists");
        }

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setIsVerified(false);

        sendVerificationEmail(user);

        userRepository.save(user);

        return "User registered successfully";
    }

    public LoginResponse login(AuthRequest request) {
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var token = tokenService.generateToken(user);
        return LoginResponse.builder().token(token).expiresIn(jwtConfig.getJwtExpiration()).build();
    }

    public void verifyUser(VerifyUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ConflictException("Account is already verified");
        }
        if (user.getVerificationExpiresAt() == null
                || user.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("Verification code has expired");
        }
        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new BusinessValidationException("Invalid verification code");
        }

        user.setIsVerified(true);
        user.setVerificationCode(null);
        user.setVerificationExpiresAt(null);
        userRepository.save(user);
    }

    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ConflictException("Account is already verified");
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiresAt(LocalDateTime.now().plusHours(1));
        sendVerificationEmail(user);
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", user.getEmail(), e);
            throw new EmailSendException("Failed to send verification email", e);
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}