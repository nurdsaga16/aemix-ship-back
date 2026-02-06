package com.example.aemix.dto.requests;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserOrderTitleRequest {
    @Size(max = 255, message = "Название заказа не должно превышать 255 символов")
    private String title;
}
