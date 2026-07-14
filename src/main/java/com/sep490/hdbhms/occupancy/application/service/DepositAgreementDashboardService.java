package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.FloorRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
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
public class DepositAgreementDashboardService {
    static final List<DepositAgreementStatus> ACTIVE_STATUSES = List.of(
            DepositAgreementStatus.PAID,
            DepositAgreementStatus.CONFIRMED,
            DepositAgreementStatus.EXTENDED
    );
    static final List<DepositAgreementStatus> MANAGED_STATUSES = List.of(
            DepositAgreementStatus.PAID,
            DepositAgreementStatus.CONFIRMED,
            DepositAgreementStatus.EXTENDED,
            DepositAgreementStatus.CONVERTED_TO_LEASE,
            DepositAgreementStatus.REFUNDED,
            DepositAgreementStatus.FORFEITED
    );

    UserRepository userRepository;
    DepositAgreementRepository depositAgreementRepository;
    FloorRepository floorRepository;

    public DashboardSummary getSummary(Long userId) {
        List<Long> ids = accessibleIds(userId);
        return new DashboardSummary(
                depositAgreementRepository.sumAmountByStatuses(ids, ACTIVE_STATUSES),
                depositAgreementRepository.countByStatuses(ids, ACTIVE_STATUSES),
                depositAgreementRepository.countByStatuses(ids, List.of(DepositAgreementStatus.CONVERTED_TO_LEASE))
        );
    }

    public List<FloorOption> getFloorOptions(Long userId) {
        List<Long> ids = accessibleIds(userId);
        return depositAgreementRepository.findDistinctFloorIds(ids, MANAGED_STATUSES).stream()
                .map(floorRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(floor -> new FloorOption(floor.getId(), floor.getName()))
                .toList();
    }

    private List<Long> accessibleIds(Long userId) {
        if (userId == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNAUTHENTICATED));
        List<DepositAgreement> agreements = user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER
                ? depositAgreementRepository.findAll()
                : depositAgreementRepository.findAllAccessibleByUserId(userId);
        return agreements.stream().map(DepositAgreement::getId).toList();
    }

    public record DashboardSummary(long totalHeldAmount, long heldCount, long convertedCount) {
    }

    public record FloorOption(Long id, String name) {
    }
}
