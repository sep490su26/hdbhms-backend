package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceLineRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.InvoiceRepository;
import com.sep490.hdbhms.billingandpayment.application.port.out.PaymentIntentRepository;
import com.sep490.hdbhms.billingandpayment.domain.model.Invoice;
import com.sep490.hdbhms.billingandpayment.domain.model.InvoiceLine;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodeGenerator;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.application.port.out.SendDepositPaymentPort;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.shared.constant.DefaultConfig;
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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendDepositPaymentAdapter implements SendDepositPaymentPort {
    static final NumberFormat MONEY_FORMATTER = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    static final long DEPOSIT_AMOUNT = 2_000L;

    JavaMailSender mailSender;
    TemplateEngine templateEngine;
    RoomRepository roomRepository;
    PropertyRepository propertyRepository;
    OtpCodeGenerator otpCodeGenerator;
    InvoiceRepository invoiceRepository;
    ExternalPaymentPort externalPaymentPort;
    InvoiceLineRepository invoiceLineRepository;
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;
    DefaultConfig defaultConfig;
    Environment environment;
    ObjectMapper objectMapper;

    @Override
    public PaymentIntent execute(DepositForm depositForm, RoomHold roomHold) {
        Long depositAmount = resolveDepositAmount();
        Room room = roomRepository.findById(depositForm.getRoomId()).orElseThrow(
                () -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND)
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
                resolveDepositPaymentProvider(),
                depositAgreement.getDepositCode(),
                roomHold.getExpiresAt()
        );
        paymentIntent = paymentIntentRepository.save(paymentIntent);
        com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkoutResponse =
                externalPaymentPort.createCheckoutRequest(
                        new PaymentRequest(
                                paymentIntent.getId(),
                                paymentIntent.getAmount(),
                                paymentIntent.getPaymentContent(),
                                paymentIntent.getExpiresAt()
                        )
                );
        paymentIntent.attachQrPayload(toCheckoutPayload(checkoutResponse, paymentIntent));
        paymentIntent.attachProviderOrderCode(checkoutResponse.providerOrderCode());
        paymentIntent = paymentIntentRepository.save(paymentIntent);
        sendDepositReceiptEmail(depositForm, room, depositAmount);
        return paymentIntent;
    }

    private Long resolveDepositAmount() {
        return DEPOSIT_AMOUNT;
    }

    private PaymentIntentProvider resolveDepositPaymentProvider() {
        String provider = environment.getProperty("app.payment.provider", "vnpay");
        if ("payos".equalsIgnoreCase(provider)) {
            return PaymentIntentProvider.PAYOS;
        }
        return PaymentIntentProvider.BANK_TRANSFER;
    }

    private String toCheckoutPayload(
            com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent checkoutResponse,
            PaymentIntent paymentIntent
    ) {
        if (checkoutResponse == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("provider", checkoutResponse.paymentIntentProvider() == null
                ? paymentIntent.getProvider().name()
                : checkoutResponse.paymentIntentProvider().name());
        payload.put("paymentStatus", checkoutResponse.paymentStatus() == null
                ? PaymentStatus.PENDING.name()
                : checkoutResponse.paymentStatus().name());
        payload.put("paymentIntentId", paymentIntent.getId());
        payload.put("orderCode", checkoutResponse.orderCode() == null ? paymentIntent.getProviderOrderCode() : checkoutResponse.orderCode());
        payload.put("paymentLinkId", checkoutResponse.paymentLinkId());
        payload.put("amount", checkoutResponse.amount() == null ? paymentIntent.getAmount() : checkoutResponse.amount());
        payload.put("providerOrderCode", checkoutResponse.providerOrderCode());
        payload.put("paymentContent", checkoutResponse.paymentContent() == null
                ? paymentIntent.getPaymentContent()
                : checkoutResponse.paymentContent());
        payload.put("description", checkoutResponse.transferDescription());
        payload.put("transferDescription", checkoutResponse.transferDescription());
        payload.put("checkoutUrl", checkoutResponse.checkOutUrl());
        payload.put("qrCode", checkoutResponse.qrCode());
        payload.put("qrPayload", checkoutResponse.qrPayload());
        payload.put("bankBin", checkoutResponse.bankBin());
        payload.put("bankShortName", checkoutResponse.bankShortName());
        payload.put("bankName", checkoutResponse.bankShortName());
        payload.put("accountNumber", checkoutResponse.accountNumber());
        payload.put("accountName", checkoutResponse.accountName());
        payload.put("receiverName", checkoutResponse.accountName());
        LocalDateTime expiresAt = checkoutResponse.expiresAt() == null
                ? paymentIntent.getExpiresAt()
                : checkoutResponse.expiresAt();
        payload.put("expiresAt", expiresAt == null ? null : expiresAt.toString());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize payment checkout payload", e);
        }
    }

    private void sendDepositReceiptEmail(DepositForm depositForm, Room room, Long depositAmount) {
        if (!StringUtils.hasText(depositForm.getEmail())) {
            log.info("Skip deposit receipt email because customer email is empty. depositFormId={}", depositForm.getId());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(depositForm.getEmail());
            helper.setSubject("[Nhà trọ Hải Đăng] Thanh toán hóa đơn đặt cọc phòng " + room.getRoomCode());
            helper.setText(generateDepositContractHtml(depositForm, room, depositAmount), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Could not send deposit receipt email. depositFormId={}, email={}", depositForm.getId(), depositForm.getEmail(), e);
        }
    }

    private String generateDepositContractHtml(DepositForm depositForm, Room room, Long depositAmount) {
        Property property = propertyRepository.findById(room.getPropertyId()).orElse(null);
        DefaultConfig.Owner owner = defaultConfig.getOwner();
        Context context = new Context();
        Map<String, Object> data = new HashMap<>();
        data.put("issuedAt", DateUtils.toddMMyyyyDateString(LocalDate.now()));
        data.put("ownerFullName", valueOrDefault(owner.getFullName(), "Hải Đăng House"));
        data.put("ownerDob", "............");
        data.put("ownerIdNumber", "............");
        data.put("ownerIdIssuedDate", "............");
        data.put("ownerIdIssuedPlace", "............");
        data.put("contactPhoneListString", valueOrDefault(owner.getPhone(), "Chưa cấu hình"));
        data.put("fullName", depositForm.getFullName());
        data.put("idNumber", depositForm.getIdNumber());
        data.put("dob", DateUtils.toddMMyyyyDateString(depositForm.getDob()));
        data.put("permanentAddress", depositForm.getPermanentAddress());
        data.put("idIssueDate", DateUtils.toddMMyyyyDateString(depositForm.getIdIssueDate()));
        data.put("idIssuePlace", depositForm.getIdIssuePlace());
        data.put("phone", depositForm.getPhone());
        data.put("roomNumber", room.getRoomCode());
        data.put("propertyAddress", property == null ? "............" : valueOrDefault(property.getAddressLine(), "............"));
        data.put("occupantNumber", room.getMaxOccupants() == null ? "............" : room.getMaxOccupants().toString());
        data.put("listedPrice", formatMoney(room.getListedPrice()));
        data.put("expectedLeaseSignDate", DateUtils.toddMMyyyyDateString(depositForm.getExpectedLeaseSignDate()));
        data.put("expectedMoveInDateString", DateUtils.toVietnameseDateString(depositForm.getExpectedMoveInDate()));
        data.put("depositAmount", formatMoney(depositAmount));
        data.put("depositAmountString", amountText(depositAmount));
        data.put("depositSignedDateString", DateUtils.toVietnameseDateString(LocalDate.now()));
        context.setVariables(data);
        return templateEngine.process("deposit-contract-template", context);
    }

    private String formatMoney(Long amount) {
        return MONEY_FORMATTER.format(amount == null ? 0L : amount) + " VND";
    }

    private String amountText(Long amount) {
        long safeAmount = amount == null ? 0L : amount;
        String text = com.sep490.hdbhms.shared.utils.StringUtils.toVietnamesePriceString(safeAmount);
        return text.isBlank()
                ? "Không đồng"
                : Character.toUpperCase(text.charAt(0)) + text.substring(1) + " đồng";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
