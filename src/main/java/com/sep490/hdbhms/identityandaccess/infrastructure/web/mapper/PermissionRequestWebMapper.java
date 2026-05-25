package com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper;

import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestApprovalResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestRejectionResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface PermissionRequestWebMapper {

    PermissionRequestResponse toResponse(PermissionRequest permissionRequest);

    PermissionRequestApprovalResponse toApprovalResponse(PermissionRequest permissionRequest);

    PermissionRequestRejectionResponse toRejectionResponse(PermissionRequest permissionRequest);

}
