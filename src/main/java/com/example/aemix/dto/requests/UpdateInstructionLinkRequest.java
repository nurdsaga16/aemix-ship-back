package com.example.aemix.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateInstructionLinkRequest {

    @NotBlank(message = "Ссылка обязательна")
    @Size(max = 2048)
    private String link;

    @Size(max = 500)
    private String subtitle;
}
