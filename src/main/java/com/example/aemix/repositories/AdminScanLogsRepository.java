package com.example.aemix.repositories;

import com.example.aemix.entities.ScanLogs;
import com.example.aemix.entities.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AdminScanLogsRepository extends JpaRepository<ScanLogs, Long> {

    @EntityGraph(attributePaths = {"order", "order.city", "user"})
    @Query("""
        SELECT l FROM ScanLogs l
        LEFT JOIN l.order o
        LEFT JOIN o.city c
        LEFT JOIN l.user u
        WHERE (CAST(:operator AS string) IS NULL OR LOWER(u.emailOrTelegramId) LIKE LOWER(CONCAT('%', CAST(:operator AS string), '%')))
          AND (CAST(:status AS string) IS NULL OR l.newStatus = :status)
          AND (CAST(:cityId AS long) IS NULL OR c.id = :cityId)
          AND (CAST(:fromDate AS localdatetime) IS NULL OR l.scannedAt >= :fromDate)
          AND (CAST(:toDate AS localdatetime) IS NULL OR l.scannedAt <= :toDate)
        """)
    Page<ScanLogs> findLogs(
            @Param("operator") String operator,
            @Param("cityId") Long cityId,
            @Param("status") Status status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
