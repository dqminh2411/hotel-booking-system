package com.hotelbooking.hotelservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelbooking.hotelservice.entity.OutboxEventEntity;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID>{
    List<OutboxEventEntity> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
