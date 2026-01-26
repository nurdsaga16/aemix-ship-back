package com.example.aemix.services;

import com.example.aemix.config.JwtConfig;
import com.example.aemix.dto.requests.TelegramAuthRequest;
import com.example.aemix.dto.responses.LoginResponse;
import com.example.aemix.entities.User;
import com.example.aemix.entities.enums.Role;
import com.example.aemix.exceptions.UnauthorizedException;
import com.example.aemix.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService {
    private static final String TELEGRAM_EMAIL_DOMAIN = "telegram.local";

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.auth.max-age-seconds:86400}")
    private long maxAgeSeconds;

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public LoginResponse authenticate(TelegramAuthRequest request) {
        if (!isValid(request)) {
            throw new UnauthorizedException("Invalid Telegram signature");
        }

        if (!isFresh(request.authDate())) {
            throw new UnauthorizedException("Telegram login is expired");
        }

        User user = userRepository.findByTelegramId(request.id())
                .orElseGet(() -> registerFromTelegram(request));

        String token = tokenService.generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtConfig.getJwtExpiration())
                .build();
    }

    private boolean isFresh(Long authDate) {
        if (authDate == null) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        return (now - authDate) <= maxAgeSeconds;
    }

    private boolean isValid(TelegramAuthRequest request) {
        String dataCheckString = buildDataCheckString(request);
        byte[] secretKey = sha256(botToken);
        byte[] hmac = hmacSha256(secretKey, dataCheckString);
        String hex = bytesToHex(hmac);
        return hex.equalsIgnoreCase(request.hash());
    }

    private String buildDataCheckString(TelegramAuthRequest request) {
        Map<String, String> data = new TreeMap<>();
        data.put("auth_date", String.valueOf(request.authDate()));
        data.put("id", String.valueOf(request.id()));
        if (request.firstName() != null) data.put("first_name", request.firstName());
        if (request.lastName() != null) data.put("last_name", request.lastName());
        if (request.photoUrl() != null) data.put("photo_url", request.photoUrl());
        if (request.username() != null) data.put("username", request.username());

        return data.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private User registerFromTelegram(TelegramAuthRequest request) {
        String email = "telegram_" + request.id() + "@" + TELEGRAM_EMAIL_DOMAIN;
        String password = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .telegramId(request.id())
                .telegramUsername(request.username())
                .telegramFirstName(request.firstName())
                .telegramLastName(request.lastName())
                .telegramPhotoUrl(request.photoUrl())
                .email(email)
                .password(password)
                .role(Role.USER)
                .isVerified(true)
                .build();

        return userRepository.save(user);
    }
}
