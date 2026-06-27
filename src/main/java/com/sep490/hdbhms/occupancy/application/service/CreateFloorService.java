package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateFloorCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateFloorUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.FloorRepository;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateFloorService implements CreateFloorUseCase {
    FloorRepository floorRepository;

    @Override
    public Floor execute(CreateFloorCommand command) {
        String floorCode = nextAvailableFloorCode(command.propertyId(), command.floorCode());
        Integer floorNumber = trailingNumber(floorCode);
        String floorName = command.name();
        Integer sortOrder = command.sortOrder();
        if (floorNumber != null && isGeneratedFloorName(floorName, command.floorCode())) {
            floorName = "Tầng " + floorNumber;
            sortOrder = floorNumber;
        }
        Floor floor = Floor.newFloor(
                command.propertyId(),
                floorCode,
                floorName,
                sortOrder
        );
        return floorRepository.save(floor);
    }

    private String nextAvailableFloorCode(Long propertyId, String requestedCode) {
        String code = requestedCode == null || requestedCode.isBlank() ? "F1" : requestedCode.trim();
        while (floorRepository.existsActiveByPropertyIdAndFloorCode(propertyId, code)) {
            code = incrementCode(code);
        }
        return code;
    }

    private String incrementCode(String code) {
        int end = code.length();
        int start = end;
        while (start > 0 && Character.isDigit(code.charAt(start - 1))) {
            start--;
        }
        if (start == end) {
            return code + "1";
        }

        String prefix = code.substring(0, start);
        String digits = code.substring(start);
        long nextNumber = Long.parseLong(digits) + 1;
        return prefix + String.format("%0" + digits.length() + "d", nextNumber);
    }

    private Integer trailingNumber(String code) {
        int end = code == null ? 0 : code.length();
        int start = end;
        while (start > 0 && Character.isDigit(code.charAt(start - 1))) {
            start--;
        }
        if (start == end) return null;
        return Integer.parseInt(code.substring(start));
    }

    private boolean isGeneratedFloorName(String name, String requestedCode) {
        if (name == null || name.isBlank()) return true;
        Integer requestedNumber = trailingNumber(requestedCode);
        Integer nameNumber = trailingNumber(name);
        String normalizedName = name.trim().toLowerCase();
        return requestedNumber != null
                && requestedNumber.equals(nameNumber)
                && (normalizedName.startsWith("f") || normalizedName.startsWith("t"));
    }
}
