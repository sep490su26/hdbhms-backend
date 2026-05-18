package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodeGenerator;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.LeadRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.SendDepositPaymentPort;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Lead;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendDepositPaymentAdapter implements SendDepositPaymentPort {
    JavaMailSender mailSender;
    LeadRepository leadRepository;
    TemplateEngine templateEngine;
    RoomRepository roomRepository;
    OtpCodeGenerator otpCodeGenerator;
    InvoiceRepository invoiceRepository;
    ExternalPaymentPort externalPaymentPort;
    InvoiceLineRepository invoiceLineRepository;
    PersonProfileRepository personProfileRepository;
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;

    @Override
    public PaymentIntent execute(DepositForm depositForm, Long userId) {
        Long depositAmount = 10000L;
        Room room = roomRepository.findById(depositForm.getRoomId()).orElseThrow(
                () -> new AppException(ApiErrorCode.UNDEFINED)
        );
        Lead lead = leadRepository.findByAssignedUserId(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        PersonProfile personProfile = personProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        DepositAgreement depositAgreement = DepositAgreement.newDepositAgreementForLeadUser(
                otpCodeGenerator.generate(),
                room.getId(),
                lead.getId(),
                personProfile.getId(),
                depositAmount,
                depositForm.getExpectedMoveInDate(),
                depositForm.getExpectedLeaseSignDate()
        );
        depositAgreementRepository.save(depositAgreement);
        Invoice invoice = Invoice.newDepositInvoice(otpCodeGenerator.generate(), room.getPropertyId(), room.getId());
        invoice = invoiceRepository.save(invoice);
        InvoiceLine invoiceLine = InvoiceLine.newDepositInvoiceLine(invoice.getId(), depositAmount);
        invoiceLineRepository.save(invoiceLine);
        PaymentIntent paymentIntent = PaymentIntent.newDepositPaymentIntent(
                invoice.getId(),
                depositAgreement.getId(),
                depositAmount,
                PaymentIntentProvider.BANK_TRANSFER,
                "Hóa đơn đặt cọc phòng " + room.getName()
        );
        paymentIntent = paymentIntentRepository.save(paymentIntent);
        String checkoutUrl = externalPaymentPort.createCheckoutRequest(
                new PaymentRequest(
                        String.valueOf(paymentIntent.getId()),
                        paymentIntent.getAmount(),
                        //Fix tạm webhook url
                        "localhost:8080/api/v1/webhook/vnpay/ipn"
                )
        ).checkOutUrl();
        sendDepositReceiptEmail(depositForm, room, checkoutUrl);
        return paymentIntent;
    }

    private void sendDepositReceiptEmail(DepositForm depositForm, Room room, String checkoutUrl) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(depositForm.getEmail());
            helper.setSubject("[Nhà trọ Hải Đăng] Thanh toán hóa đơn đặt cọc phòng " + room.getRoomCode());
            helper.setText(generateDepositContractHtml(depositForm, room, checkoutUrl), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    private String generateDepositContractHtml(DepositForm depositForm, Room room, String checkoutUrl) {
        Context context = new Context();
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", depositForm.getFullName());
        data.put("idNumber", depositForm.getIdNumber());
        data.put("phone", depositForm.getPhone());
        data.put("roomNumber", room.getRoomCode());
        data.put("expectedLeaseSignDate", depositForm.getExpectedLeaseSignDate());
        data.put("checkoutUrl", checkoutUrl);
        context.setVariables(data);
        return templateEngine.process("deposit-contract-template", context);
    }
}
