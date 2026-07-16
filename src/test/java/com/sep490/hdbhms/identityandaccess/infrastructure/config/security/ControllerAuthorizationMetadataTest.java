package com.sep490.hdbhms.identityandaccess.infrastructure.config.security;

import com.sep490.hdbhms.identityandaccess.infrastructure.web.controller.PermissionRequestController;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.controller.UserController;
import com.sep490.hdbhms.occupancy.infrastructure.web.controller.FloorController;
import com.sep490.hdbhms.occupancy.infrastructure.web.controller.RoomAssetController;
import com.sep490.hdbhms.occupancy.infrastructure.web.controller.RoomController;
import com.sep490.hdbhms.occupancy.infrastructure.web.controller.RoomTransferController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ControllerAuthorizationMetadataTest {

    @Test
    void accountAdministrationRemainsOwnerOnly() {
        assertAuthorization(UserController.class, "getAccount", "hasRole('OWNER')");
        assertAuthorization(UserController.class, "updateAccountStatus", "hasRole('OWNER')");
        assertAuthorization(UserController.class, "updateAccountRole", "hasRole('OWNER')");
        assertAuthorization(UserController.class, "getLoginHistory", "hasRole('OWNER')");
    }

    @Test
    void floorRoomAndAssetMutationsRemainOwnerOnly() {
        assertAuthorization(FloorController.class, "createFloor", "hasRole('OWNER')");
        assertAuthorization(FloorController.class, "deleteFloor", "hasRole('OWNER')");
        assertAuthorization(RoomController.class, "createRoom", "hasRole('OWNER')");
        assertAuthorization(RoomController.class, "updateRoom", "hasRole('OWNER')");
        assertAuthorization(RoomController.class, "deleteRoom", "hasRole('OWNER')");
        assertAuthorization(RoomAssetController.class, "createRoomAsset", "hasRole('OWNER')");
        assertAuthorization(RoomAssetController.class, "updateRoomAsset", "hasRole('OWNER')");
        assertAuthorization(RoomAssetController.class, "deleteRoomAsset", "hasRole('OWNER')");
    }

    @Test
    void operationalReadAndPermissionWorkflowsKeepExplicitRoles() {
        assertAuthorization(RoomController.class, "getRoomById", "hasAnyRole('OWNER','MANAGER')");
        assertAuthorization(RoomController.class, "getLatestMeterReadings", "hasAnyRole('OWNER','MANAGER')");
        assertAuthorization(PermissionRequestController.class, "getPermissionRequests", "hasRole('OWNER')");
        assertAuthorization(PermissionRequestController.class, "createPermissionRequest", "hasRole('MANAGER')");
        assertAuthorization(RoomTransferController.class, "requestTransfer", "hasRole('TENANT')");
        assertAuthorization(RoomTransferController.class, "approveTransfer", "hasAnyRole('OWNER','MANAGER')");
        assertAuthorization(RoomTransferController.class, "executeTransfer", "hasAnyRole('OWNER','MANAGER')");
        assertAuthorization(RoomTransferController.class, "getTransferRequest", "hasAnyRole('OWNER','MANAGER','TENANT')");
    }

    private void assertAuthorization(Class<?> controller, String methodName, String expression) {
        var method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, controller.getSimpleName() + "." + methodName + " must be protected");
        assertEquals(expression, annotation.value());
    }
}
