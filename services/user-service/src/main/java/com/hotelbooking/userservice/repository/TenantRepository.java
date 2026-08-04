package com.hotelbooking.userservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hotelbooking.userservice.entity.Tenant;
import com.hotelbooking.userservice.entity.TenantStatus;

public interface TenantRepository extends JpaRepository<Tenant, UUID>{

    @Query("""
        select t from Tenant t
        join fetch t.ownerUser u
        where t.isDeleted = false and u.deleted = false
        and (:status is null or t.status = :status)
        and (:search is null or lower(t.name) like lower(concat('%', :search, '%'))
            or lower(u.fullName) like lower(concat('%', :search, '%'))
            or lower(u.email) like lower(concat('%', :search, '%'))
            or lower(u.phone) like lower(concat('%', :search, '%'))
        )
        """)
    Page<Tenant> findByStatusAndSearch(TenantStatus status, String search, Pageable pageable);

    Boolean existsByOwnerUser_IdAndIsDeletedFalse(UUID ownerId);

    @EntityGraph(attributePaths = {"ownerUser"})
    Optional<Tenant> findByIdAndIsDeletedFalse(UUID tenantId);
}
