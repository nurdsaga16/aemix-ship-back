package com.example.aemix.controllers;

import com.example.aemix.dto.requests.UpdateInstructionLinkRequest;
import com.example.aemix.dto.responses.InstructionLinkResponse;
import com.example.aemix.services.InstructionLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequestMapping("${api.version}/admin/instruction-links")
@Tag(name = "Admin Instruction Links", description = "Управление ссылками на инструкции (только Super Admin)")
public class AdminInstructionLinkController {

    private final InstructionLinkService instructionLinkService;

    @Operation(summary = "Обновить ссылку")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ссылка обновлена"),
            @ApiResponse(responseCode = "404", description = "Ссылка не найдена"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PutMapping("/{id}")
    public ResponseEntity<InstructionLinkResponse> updateLink(
            @Parameter(description = "ID ссылки") @PathVariable Long id,
            @RequestBody @Valid UpdateInstructionLinkRequest request
    ) {
        return ResponseEntity.ok(instructionLinkService.updateLink(id, request));
    }
}
