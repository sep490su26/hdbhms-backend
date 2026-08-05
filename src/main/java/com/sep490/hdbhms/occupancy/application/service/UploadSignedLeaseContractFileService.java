package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.service.UploadFileService;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UploadSignedLeaseContractFileUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetLeaseContractManagementUseCase;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadSignedLeaseContractFileService implements UploadSignedLeaseContractFileUseCase {
    UploadFileService uploadFileService;
    JpaFileMetadataRepository fileMetadataRepository;
    JpaLeaseContractRepository leaseContractRepository;
    GetLeaseContractManagementUseCase getLeaseContractManagementUseCase;

    @Override
    public LeaseContractManagementResponse execute(Long leaseContractId, MultipartFile file, boolean replace) {
        var contract = leaseContractRepository.findById(leaseContractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hợp đồng thuê."));
        if (contract.getStatus() == LeaseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hợp đồng đã ACTIVE, không upload thay file trong luồng này.");
        }
        if (contract.getSignedFile() != null && !replace) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hợp đồng thuê đã có file đã ký. Gửi replace=true nếu muốn thay thế.");
        }
        Long currentUserId = AuthUtils.getCurrentAuthenticationId();
        var metadata = uploadFileService.execute(new UploadFileCommand(
                currentUserId,
                file,
                FileCategory.CONTRACT,
                true
        ));
        var signedFile = fileMetadataRepository.findById(metadata.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được file hợp đồng."));
        signedFile.setCategory(FileCategory.CONTRACT);
        signedFile.setSensitive(true);
        contract.setSignedFile(fileMetadataRepository.save(signedFile));
        contract.setSignedUploadedBy(currentUserId != null ? UserEntity.builder().id(currentUserId).build() : null);
        leaseContractRepository.save(contract);
        return getLeaseContractManagementUseCase.findOne(contract.getId());
    }
}
