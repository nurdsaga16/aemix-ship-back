package com.example.aemix.controllers;

import com.example.aemix.dto.VerifyUserDto;
import com.example.aemix.dto.requests.AuthRequest;
import com.example.aemix.dto.requests.TelegramAuthRequest;
import com.example.aemix.dto.responses.LoginResponse;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${api.version}/auth")
@Tag(name = "Auth", description = "Регистрация и вход в систему")
public class AuthController {
    private final AuthService authService;
    private final TelegramAuthService telegramAuthService;

    @Operation(
            summary = "Регистрация",
            description = "Создает новый аккаунт пользователя и возвращает подтверждающее сообщение"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует", content = @Content),
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
            description = "Проверяет подпись Telegram Login Widget и возвращает JWT токен"
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
            @ApiResponse(responseCode = "400", description = "Некорректный email", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "409", description = "Аккаунт уже подтвержден", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    public ResponseEntity<String> resendVerificationCode(
            @RequestParam
            @Email(message = "Email must be valid")
            @NotBlank(message = "Email is required")
            String email
    ) {
        authService.resendVerificationCode(email);
        return ResponseEntity.ok("Verification code sent");
    }
}