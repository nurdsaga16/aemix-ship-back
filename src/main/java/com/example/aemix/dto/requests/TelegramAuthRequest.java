package com.example.aemix.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TelegramAuthRequest(
        @NotNull(message = "Telegram id is required")
        Long id,
        @JsonProperty("first_name")
        String firstName,
        @JsonProperty("last_name")
        String lastName,
        String username,
        @JsonProperty("photo_url")
        String photoUrl,
        @NotNull(message = "auth_date is required")
        @JsonProperty("auth_date")
        Long authDate,
        @NotBlank(message = "hash is required")
        String hash
) {
}
