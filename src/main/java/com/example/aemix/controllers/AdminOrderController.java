package com.example.aemix.controllers;

import com.example.aemix.dto.requests.BulkReadyRequest;
import com.example.aemix.dto.requests.UploadOrdersRequest;
import com.example.aemix.dto.responses.*;
import com.example.aemix.entities.User;
import com.example.aemix.entities.enums.OrderSort;
import com.example.aemix.entities.enums.Status;
import com.example.aemix.services.AdminOrderService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("${api.version}/admin/orders")
@Tag(name = "Admin Orders", description = "Управление заказами администратором")
public class AdminOrderController {
    private final AdminOrderService adminOrderService;
    private final AuthService authService;

    @Operation(
            summary = "Импорт заказов из Excel",
            description = "Массовое создание заказов из Excel файла. Создает заказы со статусом INTERNATIONAL_SHIPPING"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказы успешно импортированы",
                    content = @Content(schema = @Schema(implementation = UploadOrdersResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "404", description = "Город не найден", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping("/upload")
    public ResponseEntity<UploadOrdersResponse> uploadOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UploadOrdersRequest request
    ) {
        User user = authService.getUser(jwt);
        return ResponseEntity.ok(adminOrderService.uploadOrders(request, user));
    }

    @Operation(
            summary = "Получить список всех заказов",
            description = "Возвращает список всех заказов с фильтрацией по трек-коду, статусу, городу, датам и пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заказов",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PaginationResponse<OrderResponse>> getOrders(
            @Parameter(description = "Поиск по трек-коду")
            @RequestParam(required = false) String trackCode,
            @Parameter(description = "Фильтр по статусу")
            @RequestParam(required = false) Status status,
            @Parameter(description = "Фильтр по ID города")
            @RequestParam(required = false) Long cityId,
            @Parameter(description = "Фильтр по дате начала (от)")
            @RequestParam(required = false) LocalDateTime fromDate,
            @Parameter(description = "Фильтр по дате окончания (до)")
            @RequestParam(required = false) LocalDateTime toDate,
            @Parameter(description = "Номер страницы (начиная с 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Сортировка: CREATED_DESC (сначала новые) или CREATED_ASC (сначала старые)")
            @RequestParam(defaultValue = "CREATED_DESC") OrderSort sort
    ) {
        var orders = adminOrderService.getOrders(trackCode, status, cityId, fromDate, toDate, page, size, sort);
        return ResponseEntity.ok(new PaginationResponse<>(orders));
    }
}
