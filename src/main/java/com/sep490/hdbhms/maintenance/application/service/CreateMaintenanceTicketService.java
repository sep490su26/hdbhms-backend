package com.sep490.hdbhms.maintenance.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.maintenance.application.port.in.command.CreateMaintenanceTicketCommand;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.CreateMaintenanceTicketUseCase;
import com.sep490.hdbhms.maintenance.application.port.out.GetRoomFromLeaseContractPort;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicketAttachment;
import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.property.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateMaintenanceTicketService implements CreateMaintenanceTicketUseCase {
    private static final int MAX_ATTACHMENTS = 3;

    TenantRepository tenantRepository;
    FileMetadataRepository fileMetadataRepository;
    MaintenanceTicketRepository maintenanceTicketRepository;
    MaintenanceTicketCodeService maintenanceTicketCodeService;
    GetRoomFromLeaseContractPort getRoomFromLeaseContractPort;
    MaintenanceTicketAttachmentRepository maintenanceTicketAttachmentRepository;
    RoomRepository roomRepository;
    LeaseContractRepository leaseContractRepository;
    LeaseContractQueryService leaseContractQueryService;

    @Override
    public MaintenanceTicket execute(CreateMaintenanceTicketCommand command) {
        Long currentSessionUserId = AuthUtils.getCurrentAuthenticationId();
        Tenant tenant = tenantRepository.findByUserId(currentSessionUserId)
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        LeaseRoom leaseRoom = resolveLeaseRoom(tenant, command.roomId());

        String category = firstNonBlank(command.category(), command.type(), "OTHER");
        String title = firstNonBlank(command.title(), category);
        TicketScope ticketScope = command.ticketScope() == null ? TicketScope.TENANT_ROOM : command.ticketScope();
        MaintenanceTicket maintenanceTicket = MaintenanceTicket.builder()
                .ticketCode(maintenanceTicketCodeService.nextCode(
                        leaseRoom.room().getRoomCode(),
                        LocalDate.now()
                ))
                .propertyId(leaseRoom.room().getPropertyId())
                .roomId(leaseRoom.room().getId())
                .contractId(leaseRoom.leaseContract() == null ? null : leaseRoom.leaseContract().getId())
                .createdById(currentSessionUserId)
                .ticketScope(ticketScope)
                .category(category)
                .title(title)
                .description(command.description())
                .repairRequested(command.repairRequested() == null || command.repairRequested())
                .build();
        maintenanceTicket = maintenanceTicketRepository.save(maintenanceTicket);

        List<Long> attachmentFileIds = command.attachmentIds();
        if (attachmentFileIds != null && !attachmentFileIds.isEmpty()) {
            attachmentFileIds = attachmentFileIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (attachmentFileIds.size() > MAX_ATTACHMENTS) {
                throw new AppException(ApiErrorCode.INVALID_REQUEST);
            }
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

    private LeaseRoom resolveLeaseRoom(Tenant tenant, Long requestedRoomId) {
        if (requestedRoomId == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        leaseContractQueryService.assertCurrentUserCanReadRoom(requestedRoomId);
        LeaseContract leaseContract = leaseContractQueryService
                .getRentalContexts(tenant.getUserId())
                .stream()
                .filter(context -> context.roomId().equals(requestedRoomId))
                .findFirst()
                .flatMap(context -> leaseContractRepository.findById(context.contractId()))
                .orElse(null);
        if (leaseContract != null) {
            return new LeaseRoom(leaseContract, getRoomFromLeaseContractPort.execute(leaseContract.getId()));
        }

        Room room = roomRepository.findById(requestedRoomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
        if (!tenant.getPropertyId().equals(room.getPropertyId())) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
        return new LeaseRoom(null, room);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!StringUtils.isEmpty(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private void validateAttachments(List<Long> fileIds) {
        List<Long> uniqueFileIds = fileIds.stream().distinct().toList();
        long count = fileMetadataRepository.countByIdInAndDeletedAtIsNull(uniqueFileIds);
        if (count != uniqueFileIds.size()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private record LeaseRoom(LeaseContract leaseContract, Room room) {
    }
}
