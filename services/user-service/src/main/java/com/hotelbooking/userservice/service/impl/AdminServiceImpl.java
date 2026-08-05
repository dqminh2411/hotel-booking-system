package com.hotelbooking.userservice.service.impl;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.userservice.dto.CreateUserAdminRequest;
import com.hotelbooking.userservice.dto.LockRequest;
import com.hotelbooking.userservice.dto.ResponseUser;
import com.hotelbooking.userservice.dto.UpdateUserRequest;
import com.hotelbooking.userservice.dto.UserResponse;
import com.hotelbooking.userservice.entity.AcountLockHistory;
import com.hotelbooking.userservice.entity.UserEntity;
import com.hotelbooking.userservice.entity.UserStatus;
import com.hotelbooking.userservice.exception.ApiException;
import com.hotelbooking.userservice.repository.AccountLockHistoryRepository;
import com.hotelbooking.userservice.repository.UserRepository;
import com.hotelbooking.userservice.service.AdminService;

import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminServiceImpl implements AdminService{

    String currentRealm = "hotel-booking-system";

    UserRepository userRepository;
    AccountLockHistoryRepository accountLockHistoryRepository;
    Keycloak keycloak;

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseUser> getAllUsers(UserStatus status, String search, Pageable pageable){
        if (search == null || search.isBlank()) search = null;

        Page<UserEntity> searchUsers = userRepository.findByStatusAndSearch(status, search, pageable);

        return searchUsers.map(
            user -> toResponseUser(user)
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserDetail(UUID userId){
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng có id = " + userId.toString()));
        
        try {
            return toUserResponse(user, getUserRoleKeycloak(userId));   
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi server keycloak");
        }
    }

    @Override
    @Transactional
    public UserResponse createUserAdmin(CreateUserAdminRequest request) {
        if (userRepository.existsByEmailIgnoreCaseOrPhone(request.email().trim(), request.phone().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_PHONE_ALREADY_EXISTS", "Email hoặc số điện thoại đã tồn tại trong hệ thống");
        }

        try {
            UsersResource usersResource = keycloak.realm(currentRealm).users();
            UserRepresentation user = new UserRepresentation();
            
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.password());
            credential.setTemporary(false);

            user.setUsername(request.email());
            user.setCredentials(Collections.singletonList(credential));
            user.setEmail(request.email());
            user.setFirstName("FirstName");
            user.setLastName("LastName");

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("fullName", Collections.singletonList(request.fullName()));
            attributes.put("phone", Collections.singletonList(request.phone()));

            user.setAttributes(attributes);
            user.setEmailVerified(true);
            user.setEnabled(true);

            Response response = usersResource.create(user);
            if (response.getStatus() == 201) {
                String keycloakId = CreatedResponseUtil.getCreatedId(response);
                
                RoleRepresentation role = keycloak.realm(currentRealm)
                                                .roles()
                                                .get(request.role().toString())
                                                .toRepresentation();

                UserResource userResource = usersResource.get(keycloakId.toString());
                userResource.roles().realmLevel().add(Collections.singletonList(role));

                UserEntity userEntity = new UserEntity(
                    UUID.fromString(keycloakId),
                    request.email(), 
                    request.phone(), 
                    request.fullName(), 
                    request.avatarUrl(), 
                    request.address(), 
                    UserStatus.ACTIVE, 
                    Instant.now(), 
                    Instant.now(),
                    false);

                userRepository.save(userEntity);
                return toUserResponse(userEntity, List.of(request.role().toString()));
            }
            else if(response.getStatus() == 409){
                throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS_ON_KEYCLOAK", "Email này đã tồn tại trên keycloak");
            }
            else{
                throw new ApiException(HttpStatus.BAD_REQUEST, "KEYCLOAK_CREATE_FAILED", "Không thể tạo tài khoản trên keycloak, status: " + response.getStatus());
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi server keycloak: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {

        if(request.fullName() == null && request.phone() == null
                && request.address() == null && request.avatarUrl() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Ít nhất 1 field phải được chỉnh sửa");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng có id = " + userId.toString()));

        if(request.fullName() != null) user.setFullName(request.fullName().trim());
        if(request.phone() != null) {
            String phone = request.phone().trim();
            if(!phone.equals(user.getPhone()) && userRepository.existsByPhoneAndIdNot(phone, userId)){
                throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS", "Số điện thoại đã tồn tại");
            }
            user.setPhone(phone);
        }
        if(request.address() != null) user.setAddress(request.address());
        if(request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        try {
            UserResource userResource = keycloak.realm(currentRealm).users().get(userId.toString());
            UserRepresentation userRepresentation = userResource.toRepresentation();

            Map<String, List<String>> attributes = userRepresentation.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }

            attributes.put("fullName", Collections.singletonList(request.fullName()));
            attributes.put("phone", Collections.singletonList(request.phone()));

            userRepresentation.setAttributes(attributes);

            userResource.update(userRepresentation);

            return toUserResponse(user, getUserRoleKeycloak(userId));   
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi server keycloak");
        }
    }

    // done keycloak
    @Override
    @Transactional
    public void deleteSafeUser(UUID userId, UUID adminId){
        if(userId.equals(adminId)){
            throw new ApiException(HttpStatus.FORBIDDEN, "CANNOT_MODIFY_SELF", "Admin không thể tự xóa tài khoản của mình");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng có id=" + userId.toString()));

        user.setDeleted(true);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        try {
            enableUserKeycloak(userId, false);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi server keycloak");
        }
    }

    //done keycloak
    @Override
    @Transactional
    public ResponseUser lockUser(UUID userId, UUID adminId, LockRequest request) {
        if(userId.equals(adminId)){
            throw new ApiException(HttpStatus.FORBIDDEN, "CANNOT_MODIFY_SELF", "Admin không thể tự khóa tài khoản của mình");
        }

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng có id=" + userId.toString()));

        if(user.getStatus() == UserStatus.LOCKED){
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_LOCKED", "Tài khoản đã bị khóa trước đó");
        }

        user.setStatus(UserStatus.LOCKED);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        AcountLockHistory newAcountLockHistory = new AcountLockHistory(
                UUID.randomUUID(),
                userId,
                adminId,
                request.reason(),
                Instant.now(), 
                null,
                false);
        accountLockHistoryRepository.save(newAcountLockHistory);

        try {
            enableUserKeycloak(userId, false);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi server keycloak");
        }

        return toResponseUser(user);
    }

    // done keycloak
    @Override
    @Transactional
    public ResponseUser unlockUser(UUID userId) {

        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng có id=" + userId.toString()));

        if(user.getStatus() == UserStatus.ACTIVE){
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_ACTIVE", "Tài khoản vẫn đang hoạt động");
        }

        AcountLockHistory acountLockHistory = accountLockHistoryRepository.findFirstByUserIdAndUnlockedAtIsNullOrderByLockedAtDesc(userId)
                    .orElse(null);

        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        if(acountLockHistory != null){
            acountLockHistory.setUnlockedAt(Instant.now());
            accountLockHistoryRepository.save(acountLockHistory);
        }
        else{
            log.warn("Không tìm thấy bản ghi khóa tài khoản gần nhất mà chưa mở khóa");
        }

        try {
            enableUserKeycloak(userId, true);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Lỗi server keycloak");
        }
        return toResponseUser(user);
    }

    private ResponseUser toResponseUser(UserEntity user) {
        return new ResponseUser(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private UserResponse toUserResponse(UserEntity user, List<String> userRoles) {
        return new UserResponse(
                user.getId(),
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getAddress(),
                user.getStatus().name(),
                userRoles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private void enableUserKeycloak(UUID userId, boolean enable){
        UserResource userResource = keycloak.realm(currentRealm).users().get(userId.toString());

        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(enable);

        userResource.update(user);
    }

    private List<String> getUserRoleKeycloak(UUID userId){
        UserResource userResource = keycloak.realm(currentRealm)
                                            .users().get(userId.toString());

        List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();
        return roles.stream()
                    .map(RoleRepresentation::getName)
                    .toList();
    }
}
