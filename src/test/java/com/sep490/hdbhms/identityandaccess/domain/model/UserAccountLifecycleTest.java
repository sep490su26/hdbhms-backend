package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAccountLifecycleTest {

    @Test
    void dormantAccountCanBeReactivatedForANewContract() {
        User user = tenantUser();
        user.changeStatus(AccountStatus.DORMANT);

        user.reactivateForContract();

        assertEquals(AccountStatus.ACTIVE, user.getStatus());
    }

    @Test
    void manuallyInactiveAccountCannotBeReactivatedAutomatically() {
        User user = tenantUser();
        user.lockAccount();

        AppException exception = assertThrows(
                AppException.class,
                user::reactivateForContract
        );

        assertEquals(ApiErrorCode.TENANT_ACCOUNT_REACTIVATION_BLOCKED, exception.getApiErrorCode());
        assertEquals(AccountStatus.INACTIVE, user.getStatus());
    }

    @Test
    void archivedAccountCannotBeReactivatedAutomatically() {
        User user = tenantUser();
        user.changeStatus(AccountStatus.ARCHIVED);

        AppException exception = assertThrows(
                AppException.class,
                user::reactivateForContract
        );

        assertEquals(ApiErrorCode.TENANT_ACCOUNT_REACTIVATION_BLOCKED, exception.getApiErrorCode());
        assertEquals(AccountStatus.ARCHIVED, user.getStatus());
    }

    private User tenantUser() {
        User user = User.newUser("0900000000", "tenant@example.com", "hash", Role.TENANT);
        user.activeAccount();
        return user;
    }
}
