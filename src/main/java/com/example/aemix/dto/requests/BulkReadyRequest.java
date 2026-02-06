package com.example.aemix.dto.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BulkReadyRequest {
    @NotEmpty(message = "Список трек-кодов не может быть пустым")
    @Size(max = 1000, message = "Максимальное количество заказов за одну операцию: 1000")
    private List<@NotEmpty(message = "Трек-код не может быть пустым") String> trackCodes;
}
