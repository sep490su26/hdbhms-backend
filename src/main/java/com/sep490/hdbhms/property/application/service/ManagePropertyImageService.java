package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.property.application.port.in.command.AttachPropertyImageCommand;
import com.sep490.hdbhms.property.application.port.in.command.DeletePropertyImageCommand;
import com.sep490.hdbhms.property.application.port.in.usecase.AttachPropertyImageUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.DeletePropertyImageUseCase;
import com.sep490.hdbhms.property.application.port.out.PropertyImageRepository;
import com.sep490.hdbhms.property.application.port.out.PropertyRepository;
import com.sep490.hdbhms.property.domain.model.PropertyImage;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManagePropertyImageService implements AttachPropertyImageUseCase, DeletePropertyImageUseCase {
    PropertyRepository propertyRepository;
    PropertyImageRepository propertyImageRepository;
    FileMetadataRepository fileMetadataRepository;

    @Override
    @Transactional
    public PropertyImage execute(AttachPropertyImageCommand command) {
        propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));
        FileMetadata file = fileMetadataRepository.findById(command.fileId())
                .orElseThrow(() -> new AppException(ApiErrorCode.FILE_DOWNLOAD_FAILED));
        if (file.getCategory() != FileCategory.PROPERTY_IMAGE) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }

        return propertyImageRepository.save(PropertyImage.builder()
                .propertyId(command.propertyId())
                .fileId(command.fileId())
                .sortOrder(resolveSortOrder(command.propertyId(), command.sortOrder()))
                .build());
    }

    @Override
    @Transactional
    public void execute(DeletePropertyImageCommand command) {
        PropertyImage image = propertyImageRepository.findById(command.imageId())
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_IMAGE_NOT_FOUND));
        if (!Objects.equals(image.getPropertyId(), command.propertyId())) {
            throw new AppException(ApiErrorCode.PROPERTY_IMAGE_NOT_FOUND);
        }
        propertyImageRepository.deleteById(command.imageId());
    }

    private int resolveSortOrder(Long propertyId, Integer requestedSortOrder) {
        if (requestedSortOrder != null && requestedSortOrder >= 0) {
            return requestedSortOrder;
        }
        return propertyImageRepository.findAllByPropertyId(propertyId).stream()
                .map(PropertyImage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(value -> value + 1)
                .orElse(0);
    }
}
