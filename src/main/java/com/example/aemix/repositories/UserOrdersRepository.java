package com.example.aemix.repositories;

import com.example.aemix.entities.User;
import com.example.aemix.entities.UserOrders;
import com.example.aemix.entities.enums.Status;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrdersRepository extends JpaRepository<UserOrders, Long> {
    
    Optional<UserOrders> findByOrderTrackCode(String trackCode);
    
    boolean existsByOrderTrackCode(String trackCode);
    
    @Query(value = """
    SELECT uo FROM UserOrders uo
    LEFT JOIN uo.order o
    LEFT JOIN o.city c
    WHERE uo.user.id = :userId
      AND (CAST(:text AS string) IS NULL OR LOWER(o.trackCode) LIKE LOWER(CONCAT('%', CAST(:text AS string), '%')) OR LOWER(uo.title) LIKE LOWER(CONCAT('%', CAST(:text AS string), '%')))
      AND (CAST(:status AS string) IS NULL OR o.status = :status)
      AND (CAST(:cityId AS long) IS NULL OR c.id = :cityId)
      AND (CAST(:fromDate AS localdatetime) IS NULL OR o.createdAt >= :fromDate)
      AND (CAST(:toDate AS localdatetime) IS NULL OR o.createdAt <= :toDate)
    """,
    countQuery = """
    SELECT COUNT(uo) FROM UserOrders uo
    LEFT JOIN uo.order o
    LEFT JOIN o.city c
    WHERE uo.user.id = :userId
      AND (CAST(:text AS string) IS NULL OR LOWER(o.trackCode) LIKE LOWER(CONCAT('%', CAST(:text AS string), '%')) OR LOWER(uo.title) LIKE LOWER(CONCAT('%', CAST(:text AS string), '%')))
      AND (CAST(:status AS string) IS NULL OR o.status = :status)
      AND (CAST(:cityId AS long) IS NULL OR c.id = :cityId)
      AND (CAST(:fromDate AS localdatetime) IS NULL OR o.createdAt >= :fromDate)
      AND (CAST(:toDate AS localdatetime) IS NULL OR o.createdAt <= :toDate)
    """)
    Page<UserOrders> findUserOrders(
            @Param("userId") Long userId,
            @Param("text") String text,
            @Param("status") Status status,
            @Param("cityId") Long cityId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(uo) FROM UserOrders uo
        JOIN uo.order o
        WHERE uo.user.id = :userId
          AND o.status IN (:statuses)
    """)
    long countByUserIdAndOrderStatusIn(
            @Param("userId") Long userId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
            value = """
        SELECT uo
        FROM UserOrders uo
        LEFT JOIN uo.order o
        LEFT JOIN o.city c
        WHERE uo.user = :user
        """,
            countQuery = """
        SELECT COUNT(uo)
        FROM UserOrders uo
        LEFT JOIN uo.order o
        LEFT JOIN o.city c
        WHERE uo.user = :user
        """
    )
    Page<UserOrders> findUserOrdersByUser(
            @Param("user") User user,
            Pageable pageable
    );

}
