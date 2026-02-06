package com.example.aemix.services;

import com.example.aemix.dto.requests.AddUserOrderRequest;
import com.example.aemix.dto.responses.OrderResponse;
import com.example.aemix.entities.Order;
import com.example.aemix.entities.User;
import com.example.aemix.entities.UserOrders;
import com.example.aemix.entities.enums.OrderSort;
import com.example.aemix.entities.enums.Status;
import com.example.aemix.exceptions.BusinessValidationException;
import com.example.aemix.exceptions.ConflictException;
import com.example.aemix.exceptions.ResourceNotFoundException;
import com.example.aemix.mappers.OrderMapper;
import com.example.aemix.repositories.OrderRepository;
import com.example.aemix.repositories.UserOrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserOrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserOrdersRepository userOrdersRepository;

    public Page<OrderResponse> getMyOrders(
            Long userId,
            String text,
            Status status,
            Long cityId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size,
            OrderSort orderSort
    ) {
        Sort sort;
        if (orderSort == null || orderSort == OrderSort.CREATED_DESC) {
            sort = Sort.by(Sort.Direction.DESC, "order.createdAt"); // сначала новые
        } else if (orderSort == OrderSort.CREATED_ASC) {
            sort = Sort.by(Sort.Direction.ASC, "order.createdAt");  // сначала старые
        } else {
            sort = Sort.by(Sort.Direction.DESC, "order.createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserOrders> userOrdersPage = userOrdersRepository.findUserOrders(
                userId, text, status, cityId, fromDate, toDate, pageable
        );

        return userOrdersPage.map(userOrder -> {
            Order order = userOrder.getOrder();
            OrderResponse response = orderMapper.toDto(order);
            response.setTitle(userOrder.getTitle());
            return response;
        });
    }

    public long getActiveOrdersCount(Long userId) {
        List<Status> activeStatuses = Arrays.asList(Status.INTERNATIONAL_SHIPPING, Status.ARRIVED);
        return userOrdersRepository.countByUserIdAndOrderStatusIn(userId, activeStatuses);
    }

    @Transactional
    public OrderResponse addOrderToUser(AddUserOrderRequest request, User user) {
        String trackCode = request.getTrackCode().trim();
        
        Order order = orderRepository.findByTrackCode(trackCode)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с трек-кодом " + trackCode + " не найден"));

        if (userOrdersRepository.existsByOrderTrackCode(trackCode)) {
            throw new ConflictException("Заказ с трек-кодом " + trackCode + " уже привязан к другому пользователю");
        }

        UserOrders userOrder = UserOrders.builder()
                .user(user)
                .order(order)
                .title(request.getTitle() != null ? request.getTitle().trim() : null)
                .build();
        
        userOrdersRepository.save(userOrder);

        log.info("Пользователь {} добавил заказ {}", user.getEmailOrTelegramId(), trackCode);
        
        OrderResponse response = orderMapper.toDto(order);
        response.setTitle(userOrder.getTitle());
        return response;
    }

    @Transactional
    public OrderResponse updateOrderTitle(String trackCode, String title, User user) {
        UserOrders userOrder = userOrdersRepository.findByOrderTrackCode(trackCode)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с трек-кодом " + trackCode + " не найден"));

        if (!userOrder.getUser().getId().equals(user.getId())) {
            throw new BusinessValidationException("Вы можете изменять только свои заказы");
        }

        userOrder.setTitle(title != null ? title.trim() : null);
        userOrdersRepository.save(userOrder);

        log.info("Пользователь {} обновил название заказа {}", user.getEmailOrTelegramId(), trackCode);

        OrderResponse response = orderMapper.toDto(userOrder.getOrder());
        response.setTitle(userOrder.getTitle());
        return response;
    }
}
