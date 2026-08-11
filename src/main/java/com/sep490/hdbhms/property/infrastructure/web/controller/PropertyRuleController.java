package com.sep490.hdbhms.property.infrastructure.web.controller;

import com.sep490.hdbhms.property.application.port.in.query.GetPropertyRulesQuery;
import com.sep490.hdbhms.property.application.port.in.usecase.GetPropertyRulesUseCase;
import com.sep490.hdbhms.property.domain.value_objects.RuleStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyRuleEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRuleRepository;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.UpsertPropertyRuleRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.PropertyRuleResponse;
import com.sep490.hdbhms.property.infrastructure.web.mapper.PropertyRuleWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties/{propertyId}/rules")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyRuleController {
    GetPropertyRulesUseCase getPropertyRulesUseCase;
    PropertyRuleWebMapper propertyRuleWebMapper;
    JpaPropertyRepository jpaPropertyRepository;
    JpaPropertyRuleRepository jpaPropertyRuleRepository;

    @GetMapping
    public ApiResponse<List<PropertyRuleResponse>> getPropertyRules(@PathVariable Long propertyId) {
        return ApiResponse.<List<PropertyRuleResponse>>builder()
                .data(getPropertyRulesUseCase.execute(new GetPropertyRulesQuery(propertyId))
                        .stream()
                        .map(propertyRuleWebMapper::toResponse)
                        .toList())
                .build();
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<PropertyRuleResponse> createPropertyRule(
            @PathVariable Long propertyId,
            @RequestBody UpsertPropertyRuleRequest request
    ) {
        String ruleCode = normalizeRuleCode(request == null ? null : request.ruleCode());
        PropertyRuleEntity existing = jpaPropertyRuleRepository
                .findFirstByProperty_IdAndRuleCode(propertyId, ruleCode)
                .orElse(null);
        if (existing != null && existing.getStatus() == RuleStatus.ACTIVE) {
            throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
        }
        PropertyRuleEntity rule = existing == null
                ? PropertyRuleEntity.builder()
                .property(jpaPropertyRepository.getReferenceById(requireProperty(propertyId)))
                .ruleCode(ruleCode)
                .build()
                : existing;
        applyRequest(rule, request, ruleCode);
        rule.setStatus(RuleStatus.ACTIVE);
        return ApiResponse.<PropertyRuleResponse>builder()
                .data(toResponse(jpaPropertyRuleRepository.save(rule)))
                .build();
    }

    @PutMapping("/{ruleId}")
    @Transactional
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<PropertyRuleResponse> updatePropertyRule(
            @PathVariable Long propertyId,
            @PathVariable Long ruleId,
            @RequestBody UpsertPropertyRuleRequest request
    ) {
        PropertyRuleEntity rule = findRule(propertyId, ruleId);
        String ruleCode = normalizeRuleCode(request == null ? null : request.ruleCode());
        jpaPropertyRuleRepository.findFirstByProperty_IdAndRuleCode(propertyId, ruleCode)
                .filter(existing -> !Objects.equals(existing.getId(), ruleId))
                .ifPresent(existing -> {
                    throw new AppException(ApiErrorCode.OPERATION_CONFLICT);
                });
        applyRequest(rule, request, ruleCode);
        return ApiResponse.<PropertyRuleResponse>builder()
                .data(toResponse(jpaPropertyRuleRepository.save(rule)))
                .build();
    }

    @DeleteMapping("/{ruleId}")
    @Transactional
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<PropertyRuleResponse> deletePropertyRule(
            @PathVariable Long propertyId,
            @PathVariable Long ruleId
    ) {
        PropertyRuleEntity rule = findRule(propertyId, ruleId);
        rule.setStatus(RuleStatus.INACTIVE);
        return ApiResponse.<PropertyRuleResponse>builder()
                .data(toResponse(jpaPropertyRuleRepository.save(rule)))
                .build();
    }

    private void applyRequest(
            PropertyRuleEntity rule,
            UpsertPropertyRuleRequest request,
            String ruleCode
    ) {
        if (request == null) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        rule.setRuleCode(ruleCode);
        rule.setTitle(requireText(request.title(), "Vui lòng nhập tiêu đề nội quy.", 255));
        rule.setDescription(requireText(request.description(), "Vui lòng nhập nội dung nội quy.", 5000));
        rule.setDefaultFineAmount(normalizeFineAmount(request.defaultFineAmount()));
        rule.setSortOrder(request.sortOrder() == null ? 9999 : Math.max(0, request.sortOrder()));
        rule.setStatus(request.status() == null ? RuleStatus.ACTIVE : request.status());
    }

    private Long requireProperty(Long propertyId) {
        if (propertyId == null || !jpaPropertyRepository.existsById(propertyId)) {
            throw new AppException(ApiErrorCode.PROPERTY_NOT_FOUND);
        }
        return propertyId;
    }

    private PropertyRuleEntity findRule(Long propertyId, Long ruleId) {
        requireProperty(propertyId);
        return jpaPropertyRuleRepository.findByIdAndProperty_Id(ruleId, propertyId)
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_RULE_NOT_FOUND));
    }

    static String normalizeRuleCode(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^A-Z0-9_\\-]", "_");
        normalized = normalized.replaceAll("_+", "_");
        if (normalized.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (normalized.length() > 50) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    static Long normalizeFineAmount(Long value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return value;
    }

    private String requireText(String value, String message, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (normalized.length() > maxLength) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private PropertyRuleResponse toResponse(PropertyRuleEntity rule) {
        return PropertyRuleResponse.builder()
                .id(rule.getId())
                .propertyId(rule.getProperty() == null ? null : rule.getProperty().getId())
                .ruleCode(rule.getRuleCode())
                .title(rule.getTitle())
                .description(rule.getDescription())
                .defaultFineAmount(rule.getDefaultFineAmount())
                .sortOrder(rule.getSortOrder())
                .status(rule.getStatus())
                .build();
    }
}
