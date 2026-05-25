package com.sep490.hdbhms.modules.rule.service;

import com.sep490.hdbhms.common.exception.ApiException;
import com.sep490.hdbhms.modules.auth.service.OnboardingService;
import com.sep490.hdbhms.modules.rule.dto.PropertyRuleItemResponse;
import com.sep490.hdbhms.modules.rule.dto.PropertyRuleResponse;
import com.sep490.hdbhms.modules.rule.repository.PropertyRuleRepository;
import com.sep490.hdbhms.modules.rule.repository.PropertyRuleRepository.PropertyRuleRow;
import com.sep490.hdbhms.modules.user.entity.User;
import com.sep490.hdbhms.modules.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PropertyRuleService {

    private final UserRepository userRepository;
    private final OnboardingService onboardingService;
    private final PropertyRuleRepository propertyRuleRepository;

    public PropertyRuleService(
            UserRepository userRepository,
            OnboardingService onboardingService,
            PropertyRuleRepository propertyRuleRepository
    ) {
        this.userRepository = userRepository;
        this.onboardingService = onboardingService;
        this.propertyRuleRepository = propertyRuleRepository;
    }

    public PropertyRuleResponse getRulesForCurrentTenant(Long userId, Long tenantId) {
        User user = userRepository.findById(userId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));

        if (!onboardingService.hasActiveTenant(user, tenantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem nội quy của tenant này");
        }

        Long propertyId = propertyRuleRepository.findCurrentPropertyId(tenantId);
        if (propertyId == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "CURRENT_PROPERTY_NOT_FOUND",
                    "Không tìm thấy phòng hiện tại của tenant"
            );
        }

        List<PropertyRuleRow> rows = propertyRuleRepository.findActiveRulesByPropertyId(propertyId);
        LocalDateTime updatedAt = rows.stream()
                .map(PropertyRuleRow::updatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        List<PropertyRuleItemResponse> items = rows.stream()
                .map(this::toItemResponse)
                .toList();

        return new PropertyRuleResponse(updatedAt, items);
    }

    private PropertyRuleItemResponse toItemResponse(PropertyRuleRow row) {
        return new PropertyRuleItemResponse(
                row.id(),
                row.ruleCode(),
                row.ruleCategory(),
                row.iconKey(),
                row.title(),
                row.description(),
                row.defaultFineAmount(),
                row.fineUnit(),
                row.isHighlight(),
                row.displayNote(),
                row.sortOrder(),
                row.status()
        );
    }
}
