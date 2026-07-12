package com.sep490.hdbhms.portal.application.port.in.query;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;

public record GetDashboardQuery(Long userId, Role role) {
}
