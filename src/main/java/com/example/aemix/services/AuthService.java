package com.example.aemix.services;
import com.example.aemix.config.JwtConfig;
import com.example.aemix.dto.VerifyUserDto;
import com.example.aemix.dto.requests.AuthRequest;
import com.example.aemix.dto.requests.ChangePasswordRequest;
import com.example.aemix.dto.responses.LoginResponse;
import com.example.aemix.entities.PasswordResetToken;
import com.example.aemix.entities.UserVerification;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

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
    @Value("${app.reset-password-url}")
    private String resetPasswordUrl;

    public String register(AuthRequest request) {
        if (userRepository.findByIdentifier(request.getEmailOrTelegramId()).isPresent()) {
            throw new ConflictException("User with this identifier already exists");
        }

        User user = User.builder()
                .emailOrTelegramId(request.getEmailOrTelegramId())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        user.setIsVerified(false);
        user.setVerification(UserVerification.builder()
                .verificationCode(generateVerificationCode())
                .verificationExpiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build());

        sendVerificationEmail(user);

        userRepository.save(user);

        return "User registered successfully";
    }

    public LoginResponse login(AuthRequest request) {
        User user = userRepository.findByIdentifier(request.getEmailOrTelegramId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmailOrTelegramId(),
                        request.getPassword()
                )
        );

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            return LoginResponse.builder()
                    .token(null)
                    .expiresIn(0)
                    .isVerified(false)
                    .emailOrTelegramId(user.getEmailOrTelegramId())
                    .build();
        }

        var token = tokenService.generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtConfig.getJwtExpiration())
                .isVerified(true)
                .emailOrTelegramId(user.getEmailOrTelegramId())
                .build();
    }

    public void verifyUser(VerifyUserDto request) {
        User user = userRepository.findByIdentifier(request.getEmailOrTelegramId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ConflictException("Account is already verified");
        }
        if (user.getVerification() == null
                || user.getVerification().getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("Verification code has expired");
        }
        if (user.getVerification().getVerificationCode() == null
                || !user.getVerification().getVerificationCode().equals(request.getVerificationCode())) {
            throw new BusinessValidationException("Invalid verification code");
        }

        user.setIsVerified(true);
        user.setVerification(null);
        userRepository.save(user);
    }

    public void resendVerificationCode(String emailOrTelegramId) {
        User user = userRepository.findByIdentifier(emailOrTelegramId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new ConflictException("Account is already verified");
        }

        UserVerification verification = user.getVerification();
        if (verification == null) {
            verification = UserVerification.builder().user(user).build();
        }
        verification.setVerificationCode(generateVerificationCode());
        verification.setVerificationExpiresAt(LocalDateTime.now().plusHours(1));
        user.setVerification(verification);
        sendVerificationEmail(user);
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = user.getVerification().getVerificationCode();

        String htmlTemplate = loadTemplate("templates/verification-email.html");
        String htmlMessage = htmlTemplate.replace("{{verificationCode}}", verificationCode);

        try {
            emailService.sendVerificationEmail(user.getEmailOrTelegramId(), subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", user.getEmailOrTelegramId(), e);
            throw new EmailSendException("Failed to send verification email", e);
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    public void forgotPassword(String email) {
        if (email == null || !email.contains("@")) {
            throw new BusinessValidationException("Password reset is only available for users registered with email");
        }
        User user = userRepository.findByIdentifier(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getTelegramUser() != null) {
            throw new BusinessValidationException("Password reset is not available for Telegram users");
        }

        PasswordResetToken passwordResetToken = user.getPasswordResetToken();
        if (passwordResetToken == null) {
            passwordResetToken = PasswordResetToken.builder()
                    .user(user)
                    .build();
        }

        String resetToken = UUID.randomUUID().toString();
        passwordResetToken.setResetToken(resetToken);
        passwordResetToken.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        user.setPasswordResetToken(passwordResetToken);
        sendPasswordResetEmail(user, resetToken);
        userRepository.save(user);
    }

    public void resetPassword(String token, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessValidationException("Passwords do not match");
        }

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));

        PasswordResetToken passwordResetToken = user.getPasswordResetToken();
        if (passwordResetToken == null
                || passwordResetToken.getResetToken() == null
                || passwordResetToken.getResetTokenExpiresAt() == null
                || passwordResetToken.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordResetToken(null);
        userRepository.save(user);
    }

    private void sendPasswordResetEmail(User user, String resetToken) {
        String subject = "Password Reset";
        String resetLink = resetPasswordUrl + "?token=" + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);

        String htmlTemplate = loadTemplate("templates/reset-password-email.html");
        String htmlMessage = htmlTemplate.replace("{{resetLink}}", resetLink);

        try {
            emailService.sendVerificationEmail(user.getEmailOrTelegramId(), subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", user.getEmailOrTelegramId(), e);
            throw new EmailSendException("Failed to send verification email", e);
        }
    }

    private String loadTemplate(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Template not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load template: " + path, e);
        }
    }

    public void changePassword(String identifier, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessValidationException("Passwords do not match");
        }

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getTelegramUser() != null) {
            throw new BusinessValidationException("Смена пароля недоступна для пользователей, авторизованных через Telegram");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessValidationException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public User getUser(Jwt jwt) {
        String identifier = jwt.getClaimAsString("emailOrTelegramId");
        return userRepository.findByIdentifier(identifier).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
} 