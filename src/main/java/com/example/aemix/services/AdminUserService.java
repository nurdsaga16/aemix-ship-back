package com.example.aemix.services;

import com.example.aemix.dto.requests.UserUpdateRequest;
import com.example.aemix.dto.responses.AdminUserResponse;
import com.example.aemix.dto.responses.OrderResponse;
import com.example.aemix.dto.responses.PaginationResponse;
import com.example.aemix.dto.responses.UserResponse;
import com.example.aemix.entities.Order;
import com.example.aemix.entities.User;
import com.example.aemix.entities.UserOrders;
import com.example.aemix.entities.enums.Role;
import com.example.aemix.exceptions.ResourceNotFoundException;
import com.example.aemix.mappers.OrderMapper;
import com.example.aemix.mappers.UserMapper;
import com.example.aemix.repositories.UserOrdersRepository;
import com.example.aemix.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {
    private final UserRepository userRepository;
    private final UserOrdersRepository userOrdersRepository;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;

    public Page<UserResponse> getUsers(
            String text,
            Role role,
            Boolean isVerified,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return userRepository.findUsers(text, role, isVerified, pageable)
                .map(userMapper::toDto);
    }

    public UserResponse updateUser(String emailOrTelegramId, UserUpdateRequest request) {
        User user = userRepository.findByIdentifier(emailOrTelegramId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify another SUPER_ADMIN");
        }

        user.setRole(request.getRole());

        return userMapper.toDto(userRepository.save(user));
    }

    public void deleteUser(String emailOrTelegramId) {
        User user = userRepository.findByIdentifier(emailOrTelegramId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another SUPER_ADMIN");
        }

        userRepository.delete(user);
    }

    public AdminUserResponse getUser(String emailOrTelegramId, int page, int size) {
        User user = userRepository.findByIdentifier(emailOrTelegramId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<UserOrders> userOrdersPage = userOrdersRepository.findUserOrdersByUser(user, pageable);

        String emailOrTelegramUsername = emailOrTelegramId;
        if(user.getTelegramUser()!=null && !user.getTelegramUser().getTelegramUsername().isEmpty()){
            emailOrTelegramUsername = user.getTelegramUser().getTelegramUsername();
        }

        AdminUserResponse adminUserResponse = AdminUserResponse.builder()
                .emailOrTelegramUsername(emailOrTelegramUsername)
                .role(user.getRole())
                .orders(new PaginationResponse<>(userOrdersPage.map(userOrder -> {
                    Order order = userOrder.getOrder();
                    OrderResponse response = orderMapper.toDto(order);
                    response.setTitle(userOrder.getTitle());
                    return response;
                })))
                .build();

        return  adminUserResponse;
    }
}
