package com.example.aemix.controllers;

import com.example.aemix.dto.requests.UserUpdateRequest;
import com.example.aemix.dto.responses.PaginationResponse;
import com.example.aemix.dto.responses.UserResponse;
import com.example.aemix.entities.enums.Role;
import com.example.aemix.services.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("${api.version}/admin/users")
@Tag(name = "Admin Users", description = "Управление пользователями и ролями")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @Operation(
            summary = "Получить всех пользователей",
            description = "Возвращает список всех пользователей системы"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список пользователей",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PaginationResponse<UserResponse>> getUsers(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var users = adminUserService.getUsers(text, role, isVerified, page, size);
        return ResponseEntity.ok(new PaginationResponse<>(users));
    }

    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет роль, верификацию и другие поля пользователя по идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь обновлен",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @PutMapping("/{emailOrTelegramId}")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Email или Telegram ID пользователя", required = true)
            @PathVariable("emailOrTelegramId") String emailOrTelegramId,
            @RequestBody @Valid UserUpdateRequest request
    ) {
        if (isSuperAdminSelf(jwt, emailOrTelegramId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminUserService.updateUser(emailOrTelegramId, request));
    }

    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя по email или Telegram ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь удален"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @DeleteMapping("/{emailOrTelegramId}")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Email или Telegram ID пользователя", required = true)
            @PathVariable("emailOrTelegramId") String emailOrTelegramId
    ) {
        if (isSuperAdminSelf(jwt, emailOrTelegramId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        adminUserService.deleteUser(emailOrTelegramId);
        return ResponseEntity.noContent().build();
    }

    private boolean isSuperAdminSelf(Jwt jwt, String targetIdentifier) {
        if (jwt == null) return false;
        if (!"SUPER_ADMIN".equals(jwt.getClaimAsString("role"))) return false;
        String currentIdentifier = jwt.getClaimAsString("emailOrTelegramId");
        return currentIdentifier != null && currentIdentifier.equals(targetIdentifier);
    }
}
