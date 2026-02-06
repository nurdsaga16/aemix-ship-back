package com.example.aemix.controllers;

import com.example.aemix.dto.responses.CityResponse;
import com.example.aemix.entities.City;
import com.example.aemix.services.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequestMapping("${api.version}/cities")
@Tag(name = "Cities", description = "Список всех городов")
public class CityController {
    private final CityService cityService;

    @Operation(
            summary = "Получить список городов",
            description = "Возвращает список всех городов, доступных для импорта заказов и фильтрации"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список городов",
                    content = @Content(schema = @Schema(implementation = City.class))
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав", content = @Content),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CityResponse>> getCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }
}
