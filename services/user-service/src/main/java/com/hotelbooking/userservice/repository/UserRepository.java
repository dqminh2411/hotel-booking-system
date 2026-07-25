package com.hotelbooking.userservice.repository;

import com.hotelbooking.userservice.entity.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    
    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);

    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseOrPhoneOrId(String email, String phone, UUID id);

    boolean existsByPhone(String phone);
}
