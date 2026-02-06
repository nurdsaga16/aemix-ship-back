package com.example.aemix.services;

import com.example.aemix.dto.requests.UploadOrdersRequest;
import com.example.aemix.dto.responses.OrderResponse;
import com.example.aemix.dto.responses.UploadOrdersResponse;
import com.example.aemix.entities.City;
import com.example.aemix.entities.Order;
import com.example.aemix.entities.ScanLogs;
import com.example.aemix.entities.User;
import com.example.aemix.entities.enums.OrderSort;
import com.example.aemix.entities.enums.Status;
import com.example.aemix.exceptions.ResourceNotFoundException;
import com.example.aemix.mappers.OrderMapper;
import com.example.aemix.repositories.CityRepository;
import com.example.aemix.repositories.OrderRepository;
import com.example.aemix.repositories.AdminScanLogsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AdminScanLogsRepository scanLogsRepository;
    private final CityRepository cityRepository;

    public Page<OrderResponse> getOrders(
            String trackCode,
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
            sort = Sort.by(Sort.Direction.DESC, "createdAt"); // сначала новые
        } else if (orderSort == OrderSort.CREATED_ASC) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");  // сначала старые
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return orderRepository.findOrders(trackCode, status, cityId, fromDate, toDate, pageable)
                .map(orderMapper::toDto);

    }

    @Transactional
    public UploadOrdersResponse uploadOrders(UploadOrdersRequest request, User user) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("Город с ID " + request.getCityId() + " не найден"));

        int total = request.getOrders().size();
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (UploadOrdersRequest.OrderImportItem item : request.getOrders()) {
            try {
                String trackCode = item.getTrackCode();
                if (trackCode == null || trackCode.trim().isEmpty()) {
                    skipped++;
                    errors.add("Пустой трек-код");
                    continue;
                }

                if (orderRepository.findByTrackCode(trackCode.trim()).isPresent()) {
                    skipped++;
                    errors.add(trackCode + " - заказ уже существует");
                    continue;
                }

                Order order = Order.builder()
                        .trackCode(trackCode.trim())
                        .status(Status.INTERNATIONAL_SHIPPING)
                        .city(city)
                        .build();
                order = orderRepository.save(order);
                orderRepository.flush();
                
                order = orderRepository.findByTrackCode(trackCode.trim())
                        .orElseThrow(() -> new RuntimeException("Заказ не найден после сохранения"));

                ScanLogs scanLog = ScanLogs.builder()
                        .order(order)
                        .oldStatus(Status.UNKNOWN)
                        .newStatus(Status.INTERNATIONAL_SHIPPING)
                        .user(user)
                        .build();
                scanLogsRepository.save(scanLog);

                created++;
            } catch (Exception e) {
                log.error("Ошибка при импорте заказа {}: {}", item.getTrackCode(), e.getMessage());
                skipped++;
                errors.add(item.getTrackCode() + " - " + e.getMessage());
            }
        }

        log.info("Импорт заказов: всего {}, создано {}, пропущено {}", total, created, skipped);
        return UploadOrdersResponse.builder()
                .total(total)
                .created(created)
                .skipped(skipped)
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }
}
