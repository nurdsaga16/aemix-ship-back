package com.example.aemix.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UploadOrdersRequest {
    
    @NotNull(message = "ID города обязателен")
    private Long cityId;
    
    @NotEmpty(message = "Список заказов не может быть пустым")
    @Size(max = 10000, message = "Максимальное количество заказов за один импорт: 10000")
    @Valid
    private List<OrderImportItem> orders;

    @Data
    public static class OrderImportItem {
        
        @NotNull(message = "Трек-код обязателен")
        @Size(min = 1, max = 100, message = "Трек-код должен быть от 1 до 100 символов")
        private String trackCode;
        
        private LocalDateTime shippedAt;
    }
}
