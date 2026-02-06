package com.example.aemix.services;

import com.example.aemix.dto.requests.BulkReadyRequest;
import com.example.aemix.dto.responses.BulkOperationResponse;
import com.example.aemix.dto.responses.OrderResponse;
import com.example.aemix.entities.Order;
import com.example.aemix.entities.ScanLogs;
import com.example.aemix.entities.User;
import com.example.aemix.entities.enums.Status;
import com.example.aemix.exceptions.BusinessValidationException;
import com.example.aemix.exceptions.ResourceNotFoundException;
import com.example.aemix.mappers.OrderMapper;
import com.example.aemix.repositories.AdminScanLogsRepository;
import com.example.aemix.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminScanService {
    private final OrderRepository orderRepository;
    private final AdminScanLogsRepository scanLogsRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse scanArrived(String trackCode, User user) {
        Order order = orderRepository.findByTrackCode(trackCode)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с трек-кодом " + trackCode + " не найден"));

        if (order.getStatus() != Status.INTERNATIONAL_SHIPPING) {
            throw new BusinessValidationException(
                    "Заказ должен иметь статус INTERNATIONAL_SHIPPING. Текущий статус: " + order.getStatus()
            );
        }

        Status oldStatus = order.getStatus();
        order.setStatus(Status.ARRIVED);
        orderRepository.save(order);

        ScanLogs scanLog = ScanLogs.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(Status.ARRIVED)
                .user(user)
                .build();
        scanLogsRepository.save(scanLog);

        log.info("Заказ {} отсканирован: {} -> {} пользователем {}", trackCode, oldStatus, Status.ARRIVED, user.getEmailOrTelegramId());
        return orderMapper.toDto(order);
    }

    @Transactional
    public OrderResponse scanReady(String trackCode, User user) {
        Order order = orderRepository.findByTrackCode(trackCode)
                .orElseThrow(() -> new ResourceNotFoundException("Заказ с трек-кодом " + trackCode + " не найден"));

        if (order.getStatus() != Status.ARRIVED) {
            throw new BusinessValidationException(
                    "Заказ должен иметь статус ARRIVED. Текущий статус: " + order.getStatus()
            );
        }

        Status oldStatus = order.getStatus();
        order.setStatus(Status.READY);
        orderRepository.save(order);

        ScanLogs scanLog = ScanLogs.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(Status.READY)
                .user(user)
                .build();
        scanLogsRepository.save(scanLog);

        log.info("Заказ {} помечен как готов: {} -> {} пользователем {}", trackCode, oldStatus, Status.READY, user.getEmailOrTelegramId());
        return orderMapper.toDto(order);
    }

    @Transactional
    public BulkOperationResponse bulkReady(BulkReadyRequest request, User user) {
        List<String> trackCodes = request.getTrackCodes();
        List<Order> orders = orderRepository.findByTrackCodeInAndStatus(trackCodes, Status.ARRIVED);

        int updated = 0;
        List<String> errors = new ArrayList<>();
        List<String> processedTrackCodes = new ArrayList<>();

        for (Order order : orders) {
            try {
                Status oldStatus = order.getStatus();
                order.setStatus(Status.READY);
                orderRepository.save(order);

                ScanLogs scanLog = ScanLogs.builder()
                        .order(order)
                        .oldStatus(oldStatus)
                        .newStatus(Status.READY)
                        .user(user)
                        .build();
                scanLogsRepository.save(scanLog);

                updated++;
                processedTrackCodes.add(order.getTrackCode());
            } catch (Exception e) {
                log.error("Ошибка при обновлении заказа {}: {}", order.getTrackCode(), e.getMessage());
                errors.add(order.getTrackCode());
            }
        }

        for (String trackCode : trackCodes) {
            if (!processedTrackCodes.contains(trackCode) && !errors.contains(trackCode)) {
                errors.add(trackCode);
            }
        }

        log.info("Массовое обновление заказов: обновлено {}, ошибок {}", updated, errors.size());
        return BulkOperationResponse.builder()
                .total(trackCodes.size())
                .updated(updated)
                .skipped(trackCodes.size() - updated)
                .errors(errors)
                .build();
    }
}
