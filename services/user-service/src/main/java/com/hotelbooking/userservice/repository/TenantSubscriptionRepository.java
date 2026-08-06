package com.hotelbooking.userservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelbooking.userservice.entity.TenantSubscription;
import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID>{
    
    @EntityGraph(attributePaths = {"subscriptionPlan"}) 
    Optional<TenantSubscription> findByTenant_IdAndStatusAndIsDeletedFalse(UUID tenantId, TenantSubscriptionPlanStatus status);

    boolean existsByTenant_IdAndStatusAndIsDeletedFalse(UUID tenantId, TenantSubscriptionPlanStatus status);
    boolean existsBySubscriptionPlan_IdAndStatusAndIsDeletedFalse(UUID plandId, TenantSubscriptionPlanStatus status);

    @EntityGraph(attributePaths = {"subscriptionPlan"})
    Page<TenantSubscription> findByTenant_IdAndIsDeletedFalse(UUID tenantId, Pageable pageable);
    
    boolean existsByTenant_IdAndSubscriptionPlan_IdAndStatusAndIsDeletedFalse(UUID tenantId, UUID planId, TenantSubscriptionPlanStatus status);

    @EntityGraph(attributePaths = {"tenant", "subscriptionPlan"})
    Optional<TenantSubscription> findByIdAndIsDeletedFalse(UUID id);

    List<TenantSubscription> findByExpiresAtBetweenAndStatusAndIsDeletedFalse(Instant startOfDay, Instant endOfDay, TenantSubscriptionPlanStatus status);
}
