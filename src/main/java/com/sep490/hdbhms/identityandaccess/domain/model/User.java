package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.InstantUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    final Long id;
    String phone;
    String email;
    String passwordHash;
    Role role;
    @Builder.Default
    boolean mustChangePassword = true;

    @Builder.Default
    AccountStatus status = AccountStatus.PENDING_CONTRACT;

    LocalDateTime lastLoginAt;

    final LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    public static User newUser(String phone, String email, String password, Role initialRole) {
        return User.builder()
                .email(email)
                .phone(phone)
                .passwordHash(password)
                .role(initialRole)
                .build();
    }

    public void changeEmail(String newEmail, Instant lastEmailChangedInstant) {
        if (!this.status.equals(AccountStatus.ACTIVE)) {
            throw new AppException(ApiErrorCode.ACCOUNT_IS_NOT_ACTIVE);
        }
        if (lastEmailChangedInstant != null && InstantUtils.isFixedUnitsAgoFromNow(
                lastEmailChangedInstant,
                48,
                ChronoUnit.HOURS)
        ) {
            throw new AppException(ApiErrorCode.CHANGE_USERNAME_NOT_ALLOWED_YET);
        }
        if (StringUtils.isEmpty(newEmail)) {
            throw new AppException(ApiErrorCode.INVALID_EMAIL);
        }
        if (this.email.equalsIgnoreCase(newEmail)) {
            throw new AppException(ApiErrorCode.SAME_EMAIL);
        }
        this.email = StringUtils.normalize(newEmail);
        this.updatedAt = LocalDateTime.now();
    }


    public void changePassword(String newPasswordHash) {
        if (!this.status.equals(AccountStatus.ACTIVE)) {
            throw new AppException(ApiErrorCode.ACCOUNT_IS_NOT_ACTIVE);
        }
        if (StringUtils.isEmpty(newPasswordHash)) {
            throw new AppException(ApiErrorCode.NEW_PASSWORD_IS_EMPTY);
        }
        if (this.passwordHash.equals(newPasswordHash)) {
            throw new AppException(ApiErrorCode.SAME_PASSWORD);
        }
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }


    public void changeStatus(AccountStatus newStatus) {
        if (newStatus.equals(AccountStatus.PENDING_CONTRACT)) {
            throw new AppException(ApiErrorCode.ACCOUNT_IS_NOT_VERIFIED);
        }
        if (this.status.equals(newStatus)) {
            throw new AppException(ApiErrorCode.SAME_STATUS);
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void activeAccount() {
        if (this.status == AccountStatus.ACTIVE) {
            return;
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void assignRole(Role role) {
        if (this.role.equals(role)) {
            throw new AppException(ApiErrorCode.SAME_ROLE);
        }
        this.role = role;
        updatedAt = LocalDateTime.now();
    }

    public void lockAccount() {
        if (this.status == AccountStatus.INACTIVE) {
            return;
        }
        this.status = AccountStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void registerFirstPasswordChange() {
        this.mustChangePassword = false;
        this.updatedAt = LocalDateTime.now();
    }
}
