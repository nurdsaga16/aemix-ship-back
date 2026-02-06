package com.example.aemix.controllers;

import com.example.aemix.dto.requests.BulkReadyRequest;
import com.example.aemix.dto.responses.BulkOperationResponse;
import com.example.aemix.dto.responses.OrderResponse;
import com.example.aemix.entities.User;
import com.example.aemix.services.AdminScanService;
import com.example.aemix.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${api.version}/admin/orders")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@Tag(name = "Admin Scan", description = "Логика сканирования заказов")
public class AdminScanController {
    private final AuthService authService;
    private final AdminScanService adminScanService;

    @Operation(
            summary = "Сканирование заказа при прибытии в город",
            description = "Изменяет статус заказа с INTERNATIONAL_SHIPPING на ARRIVED. Используется на странице сканирования"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ успешно отсканирован",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Некорректный статус заказа", content = @Content),
            @ApiResponse(responseCode = "404", description = "Заказ не найден", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/{trackCode}/scan-arrived")
    public ResponseEntity<OrderResponse> scanArrived(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Трек-код заказа", required = true)
            @PathVariable String trackCode
    ) {
        User user = authService.getUser(jwt);
        return ResponseEntity.ok(adminScanService.scanArrived(trackCode, user));
    }

    @Operation(
            summary = "Изменение статуса заказа на READY",
            description = "Изменяет статус заказа с ARRIVED на READY. Используется для одного заказа"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус заказа успешно изменен",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Некорректный статус заказа", content = @Content),
            @ApiResponse(responseCode = "404", description = "Заказ не найден", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/{trackCode}/scan-ready")
    public ResponseEntity<OrderResponse> scanReady(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Трек-код заказа", required = true)
            @PathVariable String trackCode
    ) {
        User user = authService.getUser(jwt);
        return ResponseEntity.ok(adminScanService.scanReady(trackCode, user));
    }

    @Operation(
            summary = "Массовое изменение статуса заказов на READY",
            description = "Изменяет статус выбранных заказов с ARRIVED на READY. Используется для массовой операции"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Операция выполнена",
                    content = @Content(schema = @Schema(implementation = BulkOperationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/bulk-ready")
    public ResponseEntity<BulkOperationResponse> bulkReady(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid BulkReadyRequest request
    ) {
        User user = authService.getUser(jwt);
        return ResponseEntity.ok(adminScanService.bulkReady(request, user));
    }
}
