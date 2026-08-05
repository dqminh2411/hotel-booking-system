package com.hotelbooking.userservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.userservice.entity.BillingCycle;
import com.hotelbooking.userservice.entity.SubscriptionPlan;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID>{

    @Query("""
            select sp from SubscriptionPlan sp
            where sp.isDeleted = false
            and (:search is null
                or lower(sp.code) like lower(concat('%', :search, '%'))
                or lower(sp.name) like lower(concat('%', :search, '%')))
            and (:billingCycle is null or sp.billingCycle = :billingCycle)
            """)
    Page<SubscriptionPlan> findBySearch(@Param("search") String search, @Param("billingCycle") BillingCycle billingCycle, Pageable pageable);

    boolean existsByCodeOrNameAndIsDeletedFalse(String code, String name);

    Optional<SubscriptionPlan> findByIdAndIsDeletedFalse(UUID planId);

    boolean existByNameAndIdNotAndIsDeletedFalse(String name, UUID planId);

}
