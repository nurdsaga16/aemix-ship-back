package com.example.aemix.controllers;

import com.example.aemix.dto.requests.AddUserOrderRequest;
import com.example.aemix.dto.requests.UpdateUserOrderTitleRequest;
import com.example.aemix.dto.responses.OrderResponse;
import com.example.aemix.dto.responses.PaginationResponse;
import com.example.aemix.entities.User;
import com.example.aemix.entities.enums.OrderSort;
import com.example.aemix.entities.enums.Status;
import com.example.aemix.services.AuthService;
import com.example.aemix.services.UserOrderService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${api.version}/user/orders")
@Tag(name = "User Orders", description = "Управление заказами пользователя")
public class UserOrderController {
    private final UserOrderService userOrderService;
    private final AuthService authService;

    @Operation(
            summary = "Получить список своих заказов",
            description = "Возвращает список заказов текущего пользователя с фильтрацией по трек-коду или названию, статусу, городу, датам и пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список заказов пользователя",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PaginationResponse<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Поиск по трек-коду или названию заказа")
            @RequestParam(required = false) String text,
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
        User user = authService.getUser(jwt);
        var orders = userOrderService.getMyOrders(user.getId(), text, status, cityId, fromDate, toDate, page, size, sort);
        return ResponseEntity.ok(new PaginationResponse<>(orders));
    }

    @Operation(
            summary = "Количество активных заказов",
            description = "Возвращает количество заказов пользователя в статусе В ПУТИ (INTERNATIONAL_SHIPPING) или ПРИБЫЛ (ARRIVED)"
    )
    @GetMapping("/active-count")
    public ResponseEntity<Long> getActiveOrdersCount(@AuthenticationPrincipal Jwt jwt) {
        User user = authService.getUser(jwt);
        long count = userOrderService.getActiveOrdersCount(user.getId());
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Добавить заказ пользователю",
            description = "Создает связь между текущим пользователем и заказом по трек-коду. Используется на странице 'Добавить заказ'"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Заказ успешно добавлен",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "404", description = "Заказ не найден", content = @Content),
            @ApiResponse(responseCode = "409", description = "Заказ уже привязан к другому пользователю", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrderResponse> addOrderToUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AddUserOrderRequest request
    ) {
        User user = authService.getUser(jwt);
        return ResponseEntity.ok(userOrderService.addOrderToUser(request, user));
    }

    @Operation(
            summary = "Обновить название заказа",
            description = "Обновляет название заказа пользователя. Пользователь может изменять только свои заказы"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Название заказа успешно обновлено",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Данные невалидны", content = @Content),
            @ApiResponse(responseCode = "404", description = "Заказ не найден", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав для изменения этого заказа", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @PutMapping("/{trackCode}/title")
    public ResponseEntity<OrderResponse> updateOrderTitle(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Трек-код заказа", required = true)
            @PathVariable String trackCode,
            @RequestBody @Valid UpdateUserOrderTitleRequest request
    ) {
        User user = authService.getUser(jwt);
        return ResponseEntity.ok(userOrderService.updateOrderTitle(trackCode, request.getTitle(), user));
    }
}
