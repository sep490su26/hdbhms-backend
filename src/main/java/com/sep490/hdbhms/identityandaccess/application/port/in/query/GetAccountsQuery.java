package com.sep490.hdbhms.identityandaccess.application.port.in.query;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import org.springframework.data.domain.Pageable;

public record GetAccountsQuery(String keyword, Role roles, AccountStatus status, Pageable pageable) {
}
