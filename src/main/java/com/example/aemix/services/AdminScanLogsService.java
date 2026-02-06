package com.example.aemix.services;

import com.example.aemix.dto.responses.ScanLogsResponse;
import com.example.aemix.entities.ScanLogs;
import com.example.aemix.mappers.ScanLogsMapper;
import com.example.aemix.entities.enums.Status;
import com.example.aemix.repositories.AdminScanLogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminScanLogsService {

    private final AdminScanLogsRepository scanLogsRepository;
    private final ScanLogsMapper scanLogsMapper;

    public Page<ScanLogsResponse> getScanLogs(
            String operator,
            Long cityId,
            Status status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scannedAt"));
        Page<ScanLogs> eventsPage = scanLogsRepository.findLogs(
                operator,
                cityId,
                status,
                fromDate,
                toDate,
                pageable
        );
        return eventsPage.map(scanLogsMapper::toDto);
    }
}

