package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.property.application.port.in.query.GetPropertyImagesByPropertyIdQuery;
import com.sep490.hdbhms.property.application.port.in.usecase.GetPropertyImagesByPropertyIdUseCase;
import com.sep490.hdbhms.property.application.port.out.PropertyImageRepository;
import com.sep490.hdbhms.property.application.port.out.PropertyRepository;
import com.sep490.hdbhms.property.domain.model.PropertyImage;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetPropertyImagesByPropertyIdService implements GetPropertyImagesByPropertyIdUseCase {
    PropertyRepository propertyRepository;
    PropertyImageRepository propertyImageRepository;

    @Override
    public List<PropertyImage> execute(GetPropertyImagesByPropertyIdQuery query) {
        propertyRepository.findById(query.propertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));
        return propertyImageRepository.findAllByPropertyId(query.propertyId());
    }
}
