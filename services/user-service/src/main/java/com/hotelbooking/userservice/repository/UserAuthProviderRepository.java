package com.hotelbooking.userservice.repository;

import com.hotelbooking.userservice.entity.UserAuthProvider;
import com.hotelbooking.userservice.entity.UserAuthProviderId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, UserAuthProviderId> {
    Optional<UserAuthProvider> findById_UserIdAndId_AuthProviderCode(
            UUID userId,
            String authProviderCode
    );
}
