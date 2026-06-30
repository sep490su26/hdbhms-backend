package com.sep490.hdbhms.maintenance.application.service;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.maintenance.application.port.in.command.CreateMaintenanceTicketCommand;
import com.sep490.hdbhms.maintenance.application.port.in.usecase.CreateMaintenanceTicketUseCase;
import com.sep490.hdbhms.maintenance.application.port.out.GetRoomFromLeaseContractPort;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicketAttachment;
import com.sep490.hdbhms.maintenance.domain.valueObjects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.valueObjects.Priority;
import com.sep490.hdbhms.maintenance.domain.valueObjects.TicketScope;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.application.port.out.LeaseContractRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateMaintenanceTicketService implements CreateMaintenanceTicketUseCase {
    private static final int MAX_ATTACHMENTS = 3;

    TenantRepository tenantRepository;
    FileMetadataRepository fileMetadataRepository;
    MaintenanceTicketRepository maintenanceTicketRepository;
    GetRoomFromLeaseContractPort getRoomFromLeaseContractPort;
    MaintenanceTicketAttachmentRepository maintenanceTicketAttachmentRepository;
    RoomRepository roomRepository;
    LeaseContractRepository leaseContractRepository;
    LeaseContractQueryService leaseContractQueryService;

    @Override
    public MaintenanceTicket execute(CreateMaintenanceTicketCommand command) {
        Long currentSessionUserId = AuthUtils.getCurrentAuthenticationId();
        Tenant tenant = tenantRepository.findByUserId(currentSessionUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin khách thuê."));
        LeaseRoom leaseRoom = resolveLeaseRoom(tenant, command.roomId());

        String category = firstNonBlank(command.category(), command.type(), "OTHER");
        String title = firstNonBlank(command.title(), category);
        TicketScope ticketScope = command.ticketScope() == null ? TicketScope.TENANT_ROOM : command.ticketScope();
        Priority priority = command.priority() == null ? Priority.MEDIUM : command.priority();

        MaintenanceTicket maintenanceTicket = MaintenanceTicket.builder()
                .ticketCode(String.format("#SC-TMP-%d-%d", currentSessionUserId, System.nanoTime()))
                .propertyId(leaseRoom.room().getPropertyId())
                .roomId(leaseRoom.room().getId())
                .contractId(leaseRoom.leaseContract() == null ? null : leaseRoom.leaseContract().getId())
                .createdById(currentSessionUserId)
                .ticketScope(ticketScope)
                .priority(priority)
                .category(category)
                .title(title)
                .description(command.description())
                .build();
        maintenanceTicket = maintenanceTicketRepository.save(maintenanceTicket);
        String ticketCode = String.format("#SC-%04d", maintenanceTicket.getId());
        maintenanceTicket.setTicketCode(ticketCode);
        maintenanceTicket = maintenanceTicketRepository.save(maintenanceTicket);

        List<Long> attachmentFileIds = command.attachmentIds();
        if (attachmentFileIds != null && !attachmentFileIds.isEmpty()) {
            attachmentFileIds = attachmentFileIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (attachmentFileIds.size() > MAX_ATTACHMENTS) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Chỉ được upload tối đa 3 ảnh trước sửa."
                );
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng đang thuê để tạo phiếu sự cố.");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy phòng đang thuê để tạo phiếu sự cố."));
        if (!tenant.getPropertyId().equals(room.getPropertyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tạo phiếu sự cố cho phòng này.");
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
            throw new IllegalArgumentException(
                    "One or more attachment files are no longer available. Please re-upload."
            );
        }
    }

    private record LeaseRoom(LeaseContract leaseContract, Room room) {
    }
}
