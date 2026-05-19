package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.IdentityDocumentRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.IdentityDocument;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.DocumentType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.occupancy.application.port.out.*;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Lead;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.RandomPasswordUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateLeadOrAssignTenantAdapter implements CreateLeadOrAssignTenantPort {
    JavaMailSender javaMailSender;
    UserRepository userRepository;
    LeadRepository leadRepository;
    RoomRepository roomRepository;
    PasswordEncoder passwordEncoder;
    TenantRepository tenantRepository;
    DepositFormRepository depositFormRepository;
    PersonProfileRepository personProfileRepository;
    DepositAgreementRepository depositAgreementRepository;
    IdentityDocumentRepository identityDocumentRepository;

    @Override
    public void execute(DepositAgreement depositAgreement) {
        DepositForm depositForm = depositFormRepository.findById(depositAgreement.getDepositFormId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        Room room = roomRepository.findById(depositForm.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        if (tenantRepository.existsByEmailOrPhone(depositForm.getEmail(), depositForm.getPhone())) {
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
        user = userRepository.save(user);
        Lead lead = Lead.newLeadUser(room.getPropertyId(), user.getId());
        lead = leadRepository.save(lead);
        PersonProfile personProfile = PersonProfile.create(
                user.getId(),
                depositForm.getFullName(),
                depositForm.getPhone(),
                depositForm.getEmail()
        );
        personProfile = personProfileRepository.save(personProfile);
        IdentityDocument identityDocument = IdentityDocument.create(
                personProfile.getId(),
                DocumentType.CCCD,
                depositForm.getIdNumber()
        );
        identityDocumentRepository.save(identityDocument);
    }
}
