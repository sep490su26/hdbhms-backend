package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.IdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.IdentityDocument;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
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
        Room room = roomRepository.findById(depositForm.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        Optional<User> existingUser = userRepository
                .findByPhoneOrEmailAndDeletedAtIsNull(
                        depositForm.getPhone(),
                        depositForm.getEmail()
                );
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            Optional<Tenant> existingTenant = tenantRepository
                    .findByUserIdAndPropertyId(user.getId(), room.getPropertyId());
            if (existingTenant.isPresent()) {
                Tenant tenant = existingTenant.get();
                PersonProfile tenantProfile = personProfileRepository.findByUserId(tenant.getUserId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
                depositAgreement.setTenantId(tenant.getId());
                depositAgreement.setDepositorPersonProfileId(tenantProfile.getId());
                depositAgreementRepository.save(depositAgreement);
                return;
            }
            Lead lead = leadRepository.findByAssignedUserId(user.getId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
            PersonProfile leadProfile = personProfileRepository.findByUserId(lead.getUserId())
                    .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
            depositAgreement.setLeadId(lead.getId());
            depositAgreement.setDepositorPersonProfileId(leadProfile.getId());
            depositAgreementRepository.save(depositAgreement);
            return;
        }

        String randomPassword = RandomPasswordUtils.generatePassword(
                6,
                true,
                true
        );
        String randomPasswordHash = passwordEncoder.encode(randomPassword);
        User user = User.newUser(
                depositForm.getPhone(),
                depositForm.getEmail(),
                randomPasswordHash,
                Role.LEAD
        );
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage().contains("uq_users_phone_active") ||
                    ex.getMessage().contains("uq_users_email_active")) {
                user = userRepository.findByPhoneOrEmailAndDeletedAtIsNull(
                                depositForm.getPhone(), depositForm.getEmail())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
            } else {
                throw ex;
            }
        }
        Lead lead = Lead.newLeadUser(room.getPropertyId(), user.getId());
        lead = leadRepository.save(lead);
        PersonProfile personProfile = PersonProfile.create(
                user.getId(),
                depositForm.getFullName(),
                depositForm.getDob(),
                depositForm.getPhone(),
                depositForm.getEmail(),
                depositForm.getPermanentAddress(),
                depositForm.getPortraitFileId()
        );
        personProfile = personProfileRepository.save(personProfile);
        if (identityDocumentRepository.existsByDocTypeAndDocNumber(
                DocumentType.CCCD, depositForm.getIdNumber())) {
            throw new AppException(ApiErrorCode.UNDEFINED);
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
        if (!StringUtils.isEmpty(depositForm.getEmail())) {
            sendPreCreatedAccountPort.sendAccountInformation(
                    depositForm.getEmail(),
                    depositForm.getFullName(),
                    depositForm.getPhone(),
                    randomPassword
            );
        }

        depositAgreement.setLeadId(lead.getId());
        depositAgreement.setDepositorPersonProfileId(personProfile.getId());
        depositAgreementRepository.save(depositAgreement);
    }
}
