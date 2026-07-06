package com.sep490.hdbhms.identityandaccess.application.port.in.query;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import org.springframework.data.domain.Pageable;

import java.util.List;

public record GetAccountsQuery(String keyword, List<Role> roles, AccountStatus status, Pageable pageable) {
}
