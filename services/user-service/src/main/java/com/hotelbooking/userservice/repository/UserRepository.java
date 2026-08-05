package com.hotelbooking.userservice.repository;

import com.hotelbooking.userservice.entity.UserEntity;
import com.hotelbooking.userservice.entity.UserStatus;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    
    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);

    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseOrPhoneOrId(String email, String phone, UUID id);

    boolean existsByPhone(String phone);

    @Query("""
            select u from UserEntity u
            where u.deleted = false
                and (:status is null or u.status = :status)
                and (:search is null
                    or lower(u.email) like lower(concat('%', :search, '%'))
                    or lower(u.phone) like lower(concat('%', :search, '%'))
                    or lower(u.fullName) like lower(concat('%', :search, '%'))
                )
            """)
    Page<UserEntity> findByStatusAndSearch(UserStatus status, String search, Pageable pageable);

    boolean existsByPhoneAndIdNot(String phone, UUID userId);
    boolean existsByEmailIgnoreCaseOrPhone(String email, String phone);
}
