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
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.SendDepositPaymentPort;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.DateUtils;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendDepositPaymentAdapter implements SendDepositPaymentPort {
    JavaMailSender mailSender;
    TemplateEngine templateEngine;
    RoomRepository roomRepository;
    OtpCodeGenerator otpCodeGenerator;
    InvoiceRepository invoiceRepository;
    ExternalPaymentPort externalPaymentPort;
    InvoiceLineRepository invoiceLineRepository;
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;

    @Override
    public PaymentIntent execute(DepositForm depositForm, RoomHold roomHold) {
        Long depositAmount = 1000000L;
        Room room = roomRepository.findById(depositForm.getRoomId()).orElseThrow(
                () -> new AppException(ApiErrorCode.UNDEFINED)
        );
        DepositAgreement depositAgreement = DepositAgreement.newDepositAgreementForLeadUser(
                otpCodeGenerator.generate(),
                room.getId(),
                depositForm.getId(),
                roomHold.getId(),
                depositAmount,
                depositForm.getExpectedMoveInDate(),
                depositForm.getExpectedLeaseSignDate(),
                roomHold.getExpiresAt()
        );
        depositAgreement = depositAgreementRepository.save(depositAgreement);
        Invoice invoice = Invoice.createDepositInvoice(
                otpCodeGenerator.generate(),
                room.getPropertyId(),
                room.getId(),
                depositAgreement.getId(),
                depositAmount,
                depositAgreement.getCreatedAt(),
                depositAgreement.getPaymentDueAt(),
                null
        );
        invoice = invoiceRepository.save(invoice);
        InvoiceLine invoiceLine = InvoiceLine.newDepositInvoiceLine(invoice.getId(), depositAmount);
        invoiceLineRepository.save(invoiceLine);
        PaymentIntent paymentIntent = PaymentIntent.newDepositPaymentIntent(
                invoice.getId(),
                depositAgreement.getId(),
                depositAmount,
                PaymentIntentProvider.BANK_TRANSFER,
                depositAgreement.getDepositCode(),
                roomHold.getExpiresAt()
        );
        paymentIntent = paymentIntentRepository.save(paymentIntent);
        String checkoutUrl = externalPaymentPort.createCheckoutRequest(
                new PaymentRequest(
                        paymentIntent.getId(),
                        paymentIntent.getAmount(),
                        paymentIntent.getPaymentContent()
                )
        ).checkOutUrl();
        sendDepositReceiptEmail(depositForm, room, checkoutUrl);
        return paymentIntent;
    }

    private void sendDepositReceiptEmail(DepositForm depositForm, Room room, String checkoutUrl) {
        if (!StringUtils.hasText(depositForm.getEmail())) {
            log.info("Skip deposit receipt email because customer email is empty. depositFormId={}", depositForm.getId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(depositForm.getEmail());
            helper.setSubject("[Nhà trọ Hải Đăng] Thanh toán hóa đơn đặt cọc phòng " + room.getRoomCode());
            helper.setText(generateDepositContractHtml(depositForm, room, checkoutUrl), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Could not send deposit receipt email. depositFormId={}, email={}", depositForm.getId(), depositForm.getEmail(), e);
        }
    }

    private String generateDepositContractHtml(DepositForm depositForm, Room room, String checkoutUrl) {
        Context context = new Context();
        Map<String, Object> data = new HashMap<>();
        data.put("issuedAt", DateUtils.toddMMyyyyDateString(LocalDate.now()));
        data.put("fullName", depositForm.getFullName());
        data.put("idNumber", depositForm.getIdNumber());
        data.put("dob", DateUtils.toddMMyyyyDateString(depositForm.getDob()));
        data.put("permanentAddress", depositForm.getPermanentAddress());
        data.put("idIssueDate", DateUtils.toddMMyyyyDateString(depositForm.getIdIssueDate()));
        data.put("idIssuePlace", depositForm.getIdIssuePlace());
        data.put("phone", depositForm.getPhone());
        data.put("roomNumber", room.getRoomCode());
        data.put("listedPrice", room.getListedPrice());
        data.put("expectedLeaseSignDate", DateUtils.toddMMyyyyDateString(depositForm.getExpectedLeaseSignDate()));
        data.put("expectedMoveInDateString", DateUtils.toVietnameseDateString(depositForm.getExpectedMoveInDate()));
        data.put("depositSignedDateString", DateUtils.toVietnameseDateString(LocalDate.now()));
        data.put("checkoutUrl", checkoutUrl);
        context.setVariables(data);
        return templateEngine.process("deposit-contract-template", context);
    }
}
