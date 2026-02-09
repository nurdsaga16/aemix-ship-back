package com.example.aemix.dto.responses;

import com.example.aemix.entities.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String trackCode;
    private Status status;
    private Long cityId;
    private String cityName;
    private String emailOrTelegramUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String title;
}
