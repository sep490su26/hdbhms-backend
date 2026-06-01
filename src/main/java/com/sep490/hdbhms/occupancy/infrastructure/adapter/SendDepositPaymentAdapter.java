package com.sep490.hdbhms.occupancy.infrastructure.adapter;

import com.lowagie.text.DocumentException;
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
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
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
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendDepositPaymentAdapter implements SendDepositPaymentPort {
    JavaMailSender mailSender;
    TemplateEngine templateEngine;
    RoomRepository roomRepository;
    UserRepository userRepository;
    OtpCodeGenerator otpCodeGenerator;
    InvoiceRepository invoiceRepository;
    ExternalPaymentPort externalPaymentPort;
    InvoiceLineRepository invoiceLineRepository;
    PaymentIntentRepository paymentIntentRepository;
    DepositAgreementRepository depositAgreementRepository;
    UploadFileUseCase uploadFileUseCase;
    PersonProfileRepository personProfileRepository;

    @Override
    public PaymentIntent execute(DepositForm depositForm, RoomHold roomHold) {
        User owner = userRepository.findOwner()
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        PersonProfile ownerProfile = personProfileRepository.findByUserId(owner.getId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        Room room = roomRepository.findById(depositForm.getRoomId()).orElseThrow(
                () -> new AppException(ApiErrorCode.UNDEFINED)
        );
        log.info("{}", depositForm);
        Long depositAmount = depositForm.getDepositMonths() * room.getListedPrice();
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

        try {
            generateAndUploadContractPdf(depositForm, room, depositAgreement, depositAmount, ownerProfile);
        } catch (Exception e) {
            log.error("Failed to generate and upload deposit contract PDF. depositFormId={}", depositForm.getId());
        }
        
        return paymentIntent;
    }

    private void generateAndUploadContractPdf(DepositForm depositForm,
                                              Room room,
                                              DepositAgreement depositAgreement,
                                              Long depositAmount,
                                              PersonProfile ownerProfile) {
        try {
            byte[] pdfBytes = generateDepositContractHtml(depositForm, room, depositAgreement,
                    depositAmount, ownerProfile);

            MultipartFile multipartFile = new ByteArrayMultipartFile(
                    "deposit_contract_" + depositAgreement.getDepositCode() + ".pdf",
                    "deposit_contract.pdf",
                    "application/pdf",
                    pdfBytes
            );

            FileMetadata fileMetadata = uploadFileUseCase.execute(
                    new UploadFileCommand(
                            null,
                            multipartFile,
                            FileCategory.DEPOSIT_CONTRACT,
                            false
                    )
            );

            depositAgreement.setContractFileId(fileMetadata.getId());
            depositAgreementRepository.save(depositAgreement);

        } catch (Exception e) {
            log.error("Failed to generate and upload deposit contract PDF. depositFormId={}",
                    depositForm.getId(), e);
            throw new RuntimeException("Could not create deposit contract PDF", e);
        }
    }

    private byte[] generateDepositContractHtml(DepositForm depositForm, Room room, DepositAgreement depositAgreement, Long depositAmount, PersonProfile ownerProfile) {
        Context context = new Context();
        Map<String, Object> data = new HashMap<>();
        data.put("issuedAt", DateUtils.toddMMyyyyDateString(LocalDate.now()));
        data.put("ownerFullName", ownerProfile.getFullName());
//        data.put("ownerDob", DateUtils.toddMMyyyyDateString(ownerProfile.getDob()));
        data.put("ownerContactPhoneListString", ownerProfile.getPhone());
        data.put("fullName", depositForm.getFullName());
        data.put("idNumber", depositForm.getIdNumber());
        data.put("dob", DateUtils.toddMMyyyyDateString(depositForm.getDob()));
        data.put("permanentAddress", depositForm.getPermanentAddress());
        data.put("idIssueDate", DateUtils.toddMMyyyyDateString(depositForm.getIdIssueDate()));
        data.put("idIssuePlace", depositForm.getIdIssuePlace());
        data.put("phone", depositForm.getPhone());
        data.put("roomNumber", room.getRoomCode());
        data.put("occupantNumber", String.valueOf(1));
        data.put("contractDuration", String.valueOf(12));
        data.put("contractStartDate", DateUtils.toVietnameseDateString(depositForm.getExpectedMoveInDate()));
        data.put("listedPrice", String.valueOf(room.getListedPrice()));
        data.put("depositMonths", String.valueOf(depositForm.getDepositMonths()));
        data.put("paymentCycleMonths", String.valueOf(depositForm.getPaymentCycleMonths()));
        data.put("depositAmount", String.valueOf(depositAmount));
        data.put("depositAmountString", StringUtils.toVietnamesePriceString(depositAmount));
        data.put("expectedLeaseSignDate", DateUtils.toddMMyyyyDateString(depositForm.getExpectedLeaseSignDate()));
        data.put("expectedMoveInDateString", DateUtils.toVietnameseDateString(depositForm.getExpectedMoveInDate()));
        data.put("depositSignedDateString", DateUtils.toVietnameseDateString(LocalDate.now()));

        context.setVariables(data);
        String htmlContent = templateEngine.process("deposit-contract-template", context);
        try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(pdfStream);
            return pdfStream.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private void replacePlaceholdersInParagraph(XWPFParagraph paragraph, Map<String, String> data) {
        String text = paragraph.getText();
        if (text == null || text.isEmpty()) return;

        boolean replaced = false;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (text.contains(placeholder)) {
                text = text.replace(placeholder, entry.getValue());
                replaced = true;
            }
        }

        if (replaced) {
            List<XWPFRun> runs = paragraph.getRuns();
            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(text);
        }
    }

    
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content == null || content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() throws IOException { return content; }

        @Override
        public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}
