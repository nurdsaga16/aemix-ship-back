package com.example.aemix.controllers;

import com.example.aemix.dto.responses.InstructionLinkResponse;
import com.example.aemix.services.InstructionLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.version}/instruction-links")
@Tag(name = "Instruction Links", description = "Ссылки на инструкции")
public class InstructionLinkController {

    private final InstructionLinkService instructionLinkService;

    @Operation(summary = "Получить все ссылки на инструкции")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список ссылок"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping
    public ResponseEntity<List<InstructionLinkResponse>> getAllLinks() {
        return ResponseEntity.ok(instructionLinkService.getAllLinks());
    }
}
