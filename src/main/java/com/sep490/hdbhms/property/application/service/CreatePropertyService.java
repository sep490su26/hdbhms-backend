package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.sep490.hdbhms.property.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.property.application.port.in.usecase.CreatePropertyUseCase;
import com.sep490.hdbhms.property.application.port.out.PropertyRepository;
import com.sep490.hdbhms.property.domain.model.Property;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreatePropertyService implements CreatePropertyUseCase {
    PropertyRepository propertyRepository;

    @Override
    public Property execute(CreatePropertyCommand command) {
        if (propertyRepository.existsByName(command.name().trim())) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        Property property = Property.newProperty(
                "CS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                command.name(),
                command.propertyType(),
                command.addressLine(),
                command.description()
        );
        return propertyRepository.save(property);
    }
}
