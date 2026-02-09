package com.example.aemix.services;

import com.example.aemix.config.JwtConfig;
import com.example.aemix.dto.requests.TelegramAuthRequest;
import com.example.aemix.dto.responses.LoginResponse;
import com.example.aemix.entities.TelegramLoginToken;
import com.example.aemix.entities.TelegramUser;
import com.example.aemix.entities.User;
import com.example.aemix.entities.enums.Role;
import com.example.aemix.exceptions.UnauthorizedException;
import com.example.aemix.repositories.TelegramLoginTokenRepository;
import com.example.aemix.repositories.TelegramUserRepository;
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
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService {
    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.auth.max-age-seconds:86400}")
    private long maxAgeSeconds;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.telegram-mini-app-link}")
    private String miniAppLink;

    private final UserRepository userRepository;
    private final TelegramLoginTokenRepository telegramLoginTokenRepository;
    private final TelegramUserRepository telegramUserRepository;
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

        User user = telegramUserRepository.findByTelegramId(request.id())
                .map(TelegramUser::getUser)
                .orElseGet(() -> registerFromTelegram(request));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            return LoginResponse.builder()
                    .token(null)
                    .expiresIn(0)
                    .isVerified(false)
                    .emailOrTelegramId(user.getEmailOrTelegramId())
                    .build();
        }

        String token = tokenService.generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .expiresIn(jwtConfig.getJwtExpiration())
                .isVerified(true)
                .emailOrTelegramId(user.getEmailOrTelegramId())
                .build();
    }

    private boolean isFresh(Long authDate) {
        if (authDate == null) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        return (now - authDate) <= maxAgeSeconds;
    }

    /**
     * Generates a signed login URL for the Telegram bot flow.
     * The bot sends this URL to the user; when clicked, the frontend callback page
     * reads params and sends them to POST /auth/telegram.
     */
    public String generateLoginLink(Long id, String firstName, String lastName, String username, String photoUrl) {
        long authDate = Instant.now().getEpochSecond();
        Map<String, String> data = new TreeMap<>();
        data.put("auth_date", String.valueOf(authDate));
        data.put("id", String.valueOf(id));
        if (firstName != null && !firstName.isBlank()) data.put("first_name", firstName);
        if (lastName != null && !lastName.isBlank()) data.put("last_name", lastName);
        if (photoUrl != null && !photoUrl.isBlank()) data.put("photo_url", photoUrl);
        if (username != null && !username.isBlank()) data.put("username", username);

        String dataCheckString = data.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        byte[] secretKey = sha256(botToken);
        byte[] hmac = hmacSha256(secretKey, dataCheckString);
        String hash = bytesToHex(hmac);

        StringBuilder url = new StringBuilder(frontendUrl).append("/telegram/callback?");
        url.append("id=").append(id);
        url.append("&auth_date=").append(authDate);
        url.append("&hash=").append(hash);
        if (firstName != null && !firstName.isBlank()) url.append("&first_name=").append(urlEncode(firstName));
        if (lastName != null && !lastName.isBlank()) url.append("&last_name=").append(urlEncode(lastName));
        if (username != null && !username.isBlank()) url.append("&username=").append(urlEncode(username));
        if (photoUrl != null && !photoUrl.isBlank()) url.append("&photo_url=").append(urlEncode(photoUrl));

        return url.toString();
    }

    /**
     * Generates a link to open the Mini App directly in Telegram.
     * Bot sends this link; when user clicks, Mini App opens with startapp param.
     */
    public String generateStartAppLoginLink(Long telegramId, String firstName, String lastName, String username) {
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().replace("-", "").getBytes(StandardCharsets.UTF_8))
                .substring(0, 32);
        Instant expiresAt = Instant.now().plusSeconds(300);

        TelegramLoginToken entity = TelegramLoginToken.builder()
                .token(token)
                .telegramId(telegramId)
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .expiresAt(expiresAt)
                .build();
        telegramLoginTokenRepository.save(entity);

        String separator = miniAppLink.contains("?") ? "&" : "?";
        return miniAppLink + separator + "startapp=" + token;
    }

    /**
     * Authenticates via Telegram Mini App initData.
     * initData передаётся при открытии Mini App из Telegram, валидируется по HMAC.
     */
    public LoginResponse authenticateByInitData(String initData) {
        long telegramId = validateInitDataAndExtractUserId(initData);
        User user = telegramUserRepository.findByTelegramId(telegramId)
                .map(TelegramUser::getUser)
                .orElseGet(() -> registerFromInitData(initData, telegramId));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            return LoginResponse.builder()
                    .token(null)
                    .expiresIn(0)
                    .isVerified(false)
                    .emailOrTelegramId(user.getEmailOrTelegramId())
                    .build();
        }

        String jwt = tokenService.generateToken(user);
        return LoginResponse.builder()
                .token(jwt)
                .expiresIn(jwtConfig.getJwtExpiration())
                .isVerified(true)
                .emailOrTelegramId(user.getEmailOrTelegramId())
                .build();
    }

    private long validateInitDataAndExtractUserId(String initData) {
        if (initData == null || initData.isBlank()) {
            throw new UnauthorizedException("Invalid initData");
        }
        Map<String, String> params = parseInitDataRaw(initData);
        String hash = params.remove("hash");
        if (hash == null || hash.isBlank()) {
            throw new UnauthorizedException("Invalid initData: missing hash");
        }
        String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        byte[] secretKey = hmacSha256WithKeyBytes("WebAppData".getBytes(StandardCharsets.UTF_8), botToken);
        byte[] calculatedHash = hmacSha256WithKeyBytes(secretKey, dataCheckString);
        String calculatedHex = bytesToHex(calculatedHash);
        if (!calculatedHex.equalsIgnoreCase(hash)) {
            throw new UnauthorizedException("Invalid initData signature");
        }
        String authDateStr = params.get("auth_date");
        if (authDateStr != null) {
            long authDate = Long.parseLong(authDateStr);
            if (!isFresh(authDate)) {
                throw new UnauthorizedException("initData expired");
            }
        }
        String userJson = params.get("user");
        if (userJson == null || userJson.isBlank()) {
            throw new UnauthorizedException("Invalid initData: missing user");
        }
        return extractUserIdFromUserJson(userJson);
    }

    private Map<String, String> parseInitDataRaw(String initData) {
        Map<String, String> result = new java.util.HashMap<>();
        for (String part : initData.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                String key = part.substring(0, eq);
                String value = eq + 1 < part.length() ? part.substring(eq + 1) : "";
                result.put(key, value);
            }
        }
        return result;
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> result = new java.util.HashMap<>();
        for (String part : initData.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                String key = java.net.URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
                String value = eq + 1 < part.length()
                        ? java.net.URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8)
                        : "";
                result.put(key, value);
            }
        }
        return result;
    }

    private long extractUserIdFromUserJson(String userJson) {
        try {
            String decoded = java.net.URLDecoder.decode(userJson, StandardCharsets.UTF_8);
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(decoded);
            var idNode = node.get("id");
            if (idNode == null || !idNode.isNumber()) {
                throw new UnauthorizedException("Invalid initData: user.id not found");
            }
            return idNode.asLong();
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid initData: cannot parse user");
        }
    }

    private User registerFromInitData(String initData, long telegramId) {
        Map<String, String> params = parseInitData(initData);
        String userJson = params.get("user");
        String firstName = null, lastName = null, username = null, photoUrl = null;
        if (userJson != null) {
            try {
                String decoded = java.net.URLDecoder.decode(userJson, StandardCharsets.UTF_8);
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(decoded);
                if (node.has("first_name")) firstName = node.get("first_name").asText(null);
                if (node.has("last_name")) lastName = node.get("last_name").asText(null);
                if (node.has("username")) username = node.get("username").asText(null);
                if (node.has("photo_url")) photoUrl = node.get("photo_url").asText(null);
            } catch (Exception ignored) {}
        }
        TelegramAuthRequest req = new TelegramAuthRequest(telegramId, firstName, lastName, username, photoUrl,
                Instant.now().getEpochSecond(), "initdata");
        return registerFromTelegram(req);
    }

    private byte[] hmacSha256WithKeyBytes(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Exchanges a one-time startapp token for a JWT. Used when Mini App opens with startapp param.
     */
    public LoginResponse authenticateByStartAppToken(String token) {
        TelegramLoginToken loginToken = telegramLoginTokenRepository
                .findByTokenAndExpiresAtAfter(token, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired login token"));

        User user = telegramUserRepository.findByTelegramId(loginToken.getTelegramId())
                .map(TelegramUser::getUser)
                .orElseGet(() -> {
                    TelegramAuthRequest authReq = new TelegramAuthRequest(
                            loginToken.getTelegramId(),
                            loginToken.getFirstName(),
                            loginToken.getLastName(),
                            loginToken.getUsername(),
                            null,
                            Instant.now().getEpochSecond(),
                            "startapp"
                    );
                    return registerFromTelegram(authReq);
                });

        telegramLoginTokenRepository.delete(loginToken);

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            return LoginResponse.builder()
                    .token(null)
                    .expiresIn(0)
                    .isVerified(false)
                    .emailOrTelegramId(user.getEmailOrTelegramId())
                    .build();
        }

        String jwt = tokenService.generateToken(user);
        return LoginResponse.builder()
                .token(jwt)
                .expiresIn(jwtConfig.getJwtExpiration())
                .isVerified(true)
                .emailOrTelegramId(user.getEmailOrTelegramId())
                .build();
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
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
        String emailOrTelegramId = String.valueOf(request.id());
        String password = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.builder()
                .emailOrTelegramId(emailOrTelegramId)
                .password(password)
                .role(Role.USER)
                .isVerified(true)
                .build();

        TelegramUser telegramUser = TelegramUser.builder()
                .telegramId(request.id())
                .telegramUsername(request.username())
                .telegramFirstName(request.firstName())
                .telegramLastName(request.lastName())
                .telegramPhotoUrl(request.photoUrl())
                .user(user)
                .build();

        user.setTelegramUser(telegramUser);
        return userRepository.save(user);
    }
}
