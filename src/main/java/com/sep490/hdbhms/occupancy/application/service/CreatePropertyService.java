package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreatePropertyUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreatePropertyService implements CreatePropertyUseCase {
    PropertyRepository propertyRepository;

    @Override
    public Property execute(CreatePropertyCommand command) {
        Property property = Property.newProperty(
                "",
                command.name(),
                command.propertyType(),
                command.addressLine(),
                command.description()
        );
        return propertyRepository.save(property);
    }
}
