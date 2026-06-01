package com.sep490.hdbhms.maintenance.application.service;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.maintenance.application.port.in.command.CreateMaintenanceTicketCommand;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.CreateMaintenanceTicketUseCase;
import com.sep490.hdbhms.maintenance.application.port.out.GetLeaseContractOfTenantPort;
import com.sep490.hdbhms.maintenance.application.port.out.GetRoomFromLeaseContractPort;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicketAttachment;
import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateMaintenanceTicketService implements CreateMaintenanceTicketUseCase {
    TenantRepository tenantRepository;
    FileMetadataRepository fileMetadataRepository;
    MaintenanceTicketRepository maintenanceTicketRepository;
    GetLeaseContractOfTenantPort getLeaseContractOfTenantPort;
    GetRoomFromLeaseContractPort getRoomFromLeaseContractPort;
    MaintenanceTicketAttachmentRepository maintenanceTicketAttachmentRepository;

    @Override
    public MaintenanceTicket execute(CreateMaintenanceTicketCommand command) {
        Long currentSessionUserId = AuthUtils.getCurrentAuthenticationId();
        Tenant tenant = tenantRepository.findByUserId(currentSessionUserId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        List<LeaseContract> activeLeaseContracts = getLeaseContractOfTenantPort.execute(tenant.getId());
        if (activeLeaseContracts.isEmpty()) {
            throw new RuntimeException("No active lease contract found");
        }
        LeaseContract leaseContract = activeLeaseContracts.getFirst();
        Room room = getRoomFromLeaseContractPort.execute(leaseContract.getId());

        //TODO: Modify enum type to match with command
        MaintenanceTicket maintenanceTicket = MaintenanceTicket.newMaintenanceTicket(
                room.getPropertyId(),
                room.getId(),
                leaseContract.getId(),
                currentSessionUserId,
                TicketScope.TENANT_ROOM,
                command.description()
        );
        maintenanceTicket = maintenanceTicketRepository.save(maintenanceTicket);
        String ticketCode = String.format("#SC-%04d", maintenanceTicket.getId());
        maintenanceTicket.setTicketCode(ticketCode);

        List<Long> attachmentFileIds = command.attachmentIds();
        if (attachmentFileIds != null && !attachmentFileIds.isEmpty()) {
            validateAttachments(attachmentFileIds);
            int sort = 0;
            for (Long fileId : attachmentFileIds) {
                MaintenanceTicketAttachment attachment = MaintenanceTicketAttachment.builder()
                        .ticketId(maintenanceTicket.getId())
                        .fileId(fileId)
                        .attachmentPhase(AttachmentPhase.BEFORE)
                        .sortOrder(sort++)
                        .createdById(currentSessionUserId)
                        .build();
                maintenanceTicketAttachmentRepository.save(attachment);
            }
        }

        return maintenanceTicket;
    }

    private void validateAttachments(List<Long> fileIds) {
        long count = fileMetadataRepository.countByIdInAndDeletedAtIsNull(fileIds);
        if (count != fileIds.size()) {
            throw new IllegalArgumentException(
                    "One or more attachment files are no longer available. Please re-upload."
            );
        }
    }
}
