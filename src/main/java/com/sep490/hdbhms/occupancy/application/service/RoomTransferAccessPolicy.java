package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RoomTransferAccessPolicy {
    private final TenantRepository tenantRepository;
    private final PersonProfileRepository personProfileRepository;
    private final LeaseContractRepository leaseContractRepository;

    public void assertCanRead(UserPrincipal principal, RoomTransferRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        if (principal.getRole() == Role.OWNER || principal.getRole() == Role.MANAGER) {
            return;
        }
        if (principal.getRole() != Role.TENANT || !isInvolvedTenant(principal.getId(), request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this transfer request");
        }
    }

    public void assertCanCancel(UserPrincipal principal, RoomTransferRequest request) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        if (principal.getRole() != Role.TENANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requesting tenant can cancel this request");
        }
        Tenant tenant = tenantRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant account not found"));
        if (!Objects.equals(tenant.getId(), request.getRequesterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the requesting tenant can cancel this request");
        }
    }

    private boolean isInvolvedTenant(Long userId, RoomTransferRequest request) {
        Tenant tenant = tenantRepository.findByUserId(userId).orElse(null);
        if (tenant != null && Objects.equals(tenant.getId(), request.getRequesterId())) {
            return true;
        }

        PersonProfile profile = personProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return false;
        }
        List<Long> transferringProfiles = request.getTransferringTenantProfileIds() == null
                ? List.of()
                : request.getTransferringTenantProfileIds();
        if (transferringProfiles.contains(profile.getId())
                || Objects.equals(request.getNominatedHolderProfileId(), profile.getId())) {
            return true;
        }
        if (request.getTargetContractId() == null) {
            return false;
        }
        return leaseContractRepository.findById(request.getTargetContractId())
                .map(contract -> Objects.equals(contract.getPrimaryTenantProfileId(), profile.getId()))
                .orElse(false);
    }
}
