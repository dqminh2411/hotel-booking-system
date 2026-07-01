package com.hotelbooking.userservice.service;

import com.hotelbooking.userservice.entity.RoleEntity;
import com.hotelbooking.userservice.entity.UserEntity;
import com.hotelbooking.userservice.entity.UserStatus;
import com.hotelbooking.userservice.exception.UserNotFoundException;
import com.hotelbooking.userservice.repository.RefreshTokenRepository;
import com.hotelbooking.userservice.repository.RoleRepository;
import com.hotelbooking.userservice.repository.UserRepository;
import com.hotelbooking.userservice.service.impl.UserServiceImpl;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void getUserByIdShouldReturnRoles() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UserServiceImpl userService = createService(userRepository);

        RoleEntity role = new RoleEntity();
        role.setId(ROLE_ID);
        role.setName("CUSTOMER");

        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setFullName("Nguyen Van An");
        user.setEmail("an.nguyen@email.com");
        user.setPhone("0901234561");
        user.setCreatedAt(Instant.parse("2024-01-15T08:30:00Z"));
        user.setUpdatedAt(Instant.parse("2024-01-15T08:30:00Z"));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role));

        Mockito.when(userRepository.findWithRolesByIdAndDeletedFalse(USER_ID))
                .thenReturn(java.util.Optional.of(user));

        var response = userService.getUserById(USER_ID);

        Assertions.assertEquals(USER_ID, response.userId());
        Assertions.assertEquals(1, response.roles().size());
        Assertions.assertEquals("CUSTOMER", response.roles().get(0));
    }

    @Test
    void getUserByIdShouldThrowWhenUserDoesNotExist() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        UserServiceImpl userService = createService(userRepository);

        UUID missingId = UUID.fromString("10000000-0000-0000-0000-000000000404");
        Mockito.when(userRepository.findWithRolesByIdAndDeletedFalse(missingId))
                .thenReturn(java.util.Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> userService.getUserById(missingId));
    }

    private UserServiceImpl createService(UserRepository userRepository) {
        return new UserServiceImpl(
                userRepository,
                Mockito.mock(RoleRepository.class),
                Mockito.mock(RefreshTokenRepository.class),
                Mockito.mock(PasswordEncoder.class),
                new TokenHashService(),
                Mockito.mock(JwtService.class),
                Mockito.mock(VerificationEmailService.class),
                Duration.ofDays(30)
        );
    }
}

