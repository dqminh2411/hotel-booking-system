package com.hotelbooking.userservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelbooking.userservice.entity.AcountLockHistory;

public interface AccountLockHistoryRepository extends JpaRepository<AcountLockHistory, UUID>{

    Optional<AcountLockHistory> findFirstByUserIdAndUnlockedAtIsNullOrderByLockedAtDesc(UUID userId);
}
