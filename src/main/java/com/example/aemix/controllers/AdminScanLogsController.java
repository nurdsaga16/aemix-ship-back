package com.example.aemix.controllers;

import com.example.aemix.dto.responses.PaginationResponse;
import com.example.aemix.dto.responses.ScanLogsResponse;
import com.example.aemix.entities.enums.Status;
import com.example.aemix.services.AdminScanLogsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("${api.version}/admin/scan-logs")
@Tag(name = "Admin Scan Logs", description = "Просмотр логов сканирования заказов")
public class AdminScanLogsController {

    private final AdminScanLogsService adminScanLogsService;

    @Operation(
            summary = "Получить логи сканирования",
            description = "Возвращает список событий сканирования заказов с пагинацией"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список логов сканирования",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<PaginationResponse<ScanLogsResponse>> getScanLogs(
            @Parameter(description = "Поиск по оператору (emailOrTelegramId)")
            @RequestParam(required = false) String operator,
            @Parameter(description = "Фильтр по ID города")
            @RequestParam(required = false) Long cityId,
            @Parameter(description = "Фильтр по новому статусу")
            @RequestParam(required = false) Status status,
            @Parameter(description = "Фильтр по дате начала (от)")
            @RequestParam(required = false) java.time.LocalDateTime fromDate,
            @Parameter(description = "Фильтр по дате окончания (до)")
            @RequestParam(required = false) java.time.LocalDateTime toDate,
            @Parameter(description = "Номер страницы (начиная с 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<ScanLogsResponse> eventsPage = adminScanLogsService.getScanLogs(
                operator,
                cityId,
                status,
                fromDate,
                toDate,
                page,
                size
        );
        return ResponseEntity.ok(new PaginationResponse<>(eventsPage));
    }
}

