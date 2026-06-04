package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.IdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.IdentityDocument;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.*;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.RandomPasswordUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateLeadOrAssignTenantAdapter implements CreateLeadOrAssignTenantPort {
    UserRepository userRepository;
    LeadRepository leadRepository;
    RoomRepository roomRepository;
    PasswordEncoder passwordEncoder;
    TenantRepository tenantRepository;
    DepositFormRepository depositFormRepository;
    PersonProfileRepository personProfileRepository;
    SendPreCreatedAccountPort sendPreCreatedAccountPort;
    DepositAgreementRepository depositAgreementRepository;
    IdentityDocumentRepository identityDocumentRepository;

    @Override
    public void execute(DepositAgreement depositAgreement) {
        DepositForm depositForm = depositFormRepository.findById(depositAgreement.getDepositFormId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        /*
         * Flow moi: deposit paid chi giu phong va tao ho so nguoi dat coc.
         * Khong tao user TENANT, khong tao tenant membership, khong gui email password o buoc nay.
         * Tai khoan chi duoc cap sau khi hop dong thue offline ACTIVE va quan ly bam gui tai khoan.
         */
        PersonProfile personProfile = ensurePersonProfile(depositForm);
        ensureIdentityDocument(personProfile, depositForm);

//        depositAgreement.setLeadId(lead.getId());
        depositAgreement.setDepositorPersonProfileId(personProfile.getId());
        depositAgreementRepository.save(depositAgreement);
    }

    private User ensureTenantAccount(User user) {
        boolean changed = false;
        if (user.getStatus() != AccountStatus.ACTIVE) {
            user.activeAccount();
            changed = true;
        }
        if (user.getRole() != Role.TENANT) {
            user.assignRole(Role.TENANT);
            changed = true;
        }
        if (changed) {
            return userRepository.save(user);
        }
        return user;
    }

    private Tenant ensureTenantMembership(Long propertyId, Long userId) {
        return tenantRepository.findByUserIdAndPropertyId(userId, propertyId)
                .orElseGet(() -> tenantRepository.save(Tenant.newTenant(propertyId, userId)));
    }

    private PersonProfile ensurePersonProfile(User user, DepositForm depositForm) {
        return personProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> personProfileRepository.save(
                        PersonProfile.create(
                                user.getId(),
                                depositForm.getFullName(),
                                depositForm.getDob(),
                                depositForm.getPhone(),
                                depositForm.getEmail(),
                                depositForm.getPermanentAddress(),
                                depositForm.getPortraitFileId()
                        )
                ));
    }

    private PersonProfile ensurePersonProfile(DepositForm depositForm) {
        return personProfileRepository.save(
                PersonProfile.create(
                        null,
                        depositForm.getFullName(),
                        depositForm.getDob(),
                        depositForm.getPhone(),
                        depositForm.getEmail(),
                        depositForm.getPermanentAddress(),
                        depositForm.getPortraitFileId()
                )
        );
    }

    private void ensureIdentityDocument(PersonProfile personProfile, DepositForm depositForm) {
        if (StringUtils.isEmpty(depositForm.getIdNumber())
                || identityDocumentRepository.existsByProfileIdAndDocType(personProfile.getId(), DocumentType.CCCD)) {
            return;
        }
        if (identityDocumentRepository.existsByDocTypeAndDocNumber(DocumentType.CCCD, depositForm.getIdNumber())) {
            log.warn("Skip creating duplicate CCCD document for deposit profile {}", personProfile.getId());
            return;
        }
        IdentityDocument identityDocument = IdentityDocument.create(
                personProfile.getId(),
                DocumentType.CCCD,
                depositForm.getIdNumber(),
                depositForm.getIdIssueDate(),
                depositForm.getIdIssuePlace(),
                depositForm.getIdFrontFileId(),
                depositForm.getIdBackFileId()
        );
        identityDocumentRepository.save(identityDocument);
    }

    private void assignTenantToDepositAgreement(
            DepositAgreement depositAgreement,
            Tenant tenant,
            PersonProfile personProfile
    ) {
        depositAgreement.setTenantId(tenant.getId());
        depositAgreement.setDepositorPersonProfileId(personProfile.getId());
        depositAgreementRepository.save(depositAgreement);
    }

    private boolean isActiveUserDuplicate(DataIntegrityViolationException ex) {
        String message = ex.getMessage();
        return message != null
                && (message.contains("uq_users_phone_active")
                || message.contains("uq_users_email_active"));
    }
}
