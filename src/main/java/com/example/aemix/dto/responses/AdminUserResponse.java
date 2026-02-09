package com.example.aemix.dto.responses;

import com.example.aemix.entities.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserResponse {
    private String emailOrTelegramUsername;
    private Role role;
    private PaginationResponse<OrderResponse> orders;
}
