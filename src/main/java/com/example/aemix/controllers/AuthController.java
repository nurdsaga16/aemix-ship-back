package com.example.aemix.controllers;

import com.example.aemix.dto.VerifyUserDto;
import com.example.aemix.dto.requests.AuthRequest;
import com.example.aemix.dto.requests.ChangePasswordRequest;
import com.example.aemix.dto.requests.ForgotPasswordRequest;
import com.example.aemix.dto.requests.ResetPasswordRequest;
import com.example.aemix.dto.requests.TelegramAuthRequest;
import com.example.aemix.dto.requests.TelegramInitDataRequest;
import com.example.aemix.dto.requests.TelegramStartAppRequest;
import com.example.aemix.dto.responses.LoginResponse;
import com.example.aemix.dto.responses.UserResponse;
import com.example.aemix.entities.User;
import com.example.aemix.mappers.UserMapper;
import com.example.aemix.services.AuthService;
import com.example.aemix.services.TelegramAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${api.version}/auth")
@Tag(name = "Auth", description = "Регистрация и вход в систему")
public class AuthController {
    private final AuthService authService;
    private final TelegramAuthService telegramAuthService;
    private final UserMapper userMapper;

    @Operation(
            summary = "Регистрация",
            description = "Создает новый аккаунт пользователя и возвращает подтверждающее сообщение"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким идентификатором уже существует", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(
            summary = "Авторизация",
            description = "Проверяет учетные данные и возвращает JWT токен для доступа к защищенным ресурсам"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Авторизация успешна",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Авторизация через Telegram",
            description = "Проверяет подпись данных, полученных по ссылке от Telegram-бота, и возвращает JWT токен"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Авторизация успешна",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неверная подпись Telegram", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/telegram")
    public ResponseEntity<LoginResponse> loginWithTelegram(
            @Valid @RequestBody TelegramAuthRequest request
    ) {
        return ResponseEntity.ok(telegramAuthService.authenticate(request));
    }

    @PostMapping("/telegram/init")
    @Operation(
            summary = "Авторизация через initData Mini App",
            description = "Валидирует initData от Telegram Web App и возвращает JWT. Вызывать при открытии Mini App."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Авторизация успешна",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Невалидная initData", content = @Content)
    })
    public ResponseEntity<LoginResponse> loginWithInitData(
            @Valid @RequestBody TelegramInitDataRequest request
    ) {
        return ResponseEntity.ok(telegramAuthService.authenticateByInitData(request.initData()));
    }

    @PostMapping("/telegram/startapp")
    @Operation(
            summary = "Авторизация через Telegram Mini App",
            description = "Обменивает одноразовый токен из startapp на JWT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Авторизация успешна",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Недействительный или просроченный токен", content = @Content)
    })
    public ResponseEntity<LoginResponse> loginWithStartApp(
            @Valid @RequestBody TelegramStartAppRequest request
    ) {
        return ResponseEntity.ok(telegramAuthService.authenticateByStartAppToken(request.token()));
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Подтверждение аккаунта",
            description = "Проверяет код подтверждения и активирует аккаунт"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт успешно подтвержден"),
            @ApiResponse(responseCode = "400", description = "Неверный код подтверждения или данные невалидны", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "409", description = "Аккаунт уже подтвержден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    public ResponseEntity<String> verifyUser(@Valid @RequestBody VerifyUserDto verifyUserDto) {
        authService.verifyUser(verifyUserDto);
        return ResponseEntity.ok("Account verified successfully");
    }

    @PostMapping("/resend")
    @Operation(
            summary = "Повторная отправка кода подтверждения",
            description = "Отправляет новый код подтверждения на email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Код подтверждения отправлен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "409", description = "Аккаунт уже подтвержден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    public ResponseEntity<String> resendVerificationCode(
            @RequestParam
            @NotBlank(message = "Email or Telegram ID is required")
            String emailOrTelegramId
    ) {
        authService.resendVerificationCode(emailOrTelegramId);
        return ResponseEntity.ok("Verification code sent");
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Сброс пароля",
            description = "Отправляет ссылку для сброса пароля на email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно сброшен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    public ResponseEntity<String> forgotPassword(
            @Valid
            @RequestBody
            ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("Password reset email sent");
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Сброс пароля",
            description = "Сбрасывает пароль пользователя по reset token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно сброшен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    public ResponseEntity<String> resetPassword(
            @Valid
            @RequestBody
            ResetPasswordRequest request
    ) {
        authService.resetPassword(request.getToken(), request.getPassword(), request.getConfirmPassword());
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Изменение пароля",
            description = "Изменяет пароль текущего авторизованного пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно изменён"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String identifier = jwt.getClaimAsString("emailOrTelegramId");
        authService.changePassword(identifier, request);
        return ResponseEntity.ok("Password changed successfully");
    }


    @GetMapping("/me")
    @Operation(summary = "Проверка сессии", description = "Возвращает данные текущего пользователя, если он авторизован и существует в БД")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        User user = authService.getUser(jwt);
        return ResponseEntity.ok(userMapper.toDto(user));
    }
}