package com.sep490.hdbhms.occupancy.application.service;

import com.lowagie.text.pdf.BaseFont;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.query.DownloadFileQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.DownloadFileUseCase;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.domain.valueObjects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileDataResponse;
import com.sep490.hdbhms.occupancy.application.port.out.DepositAgreementRepository;
import com.sep490.hdbhms.occupancy.application.port.out.DepositFormRepository;
import com.sep490.hdbhms.occupancy.application.port.out.PropertyRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.DepositContractPreviewRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.DepositContractPreviewResponse;
import com.sep490.hdbhms.shared.constant.DefaultConfig;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepositContractDocumentService {
    static final long DEFAULT_DEPOSIT_AMOUNT = 2_000L;
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    static final NumberFormat MONEY_FORMATTER = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    static final String DEPOSIT_RECEIVER_FULL_NAME = "ĐẶNG VĂN NHUẦN";
    static final String DEPOSIT_RECEIVER_DOB = "06/08/1978";
    static final String DEPOSIT_RECEIVER_ID_NUMBER = "036078008683";
    static final String DEPOSIT_RECEIVER_ID_ISSUED_DATE = "01/04/2020";
    static final String DEPOSIT_RECEIVER_ID_ISSUED_PLACE = "Cục cảnh sát QLHCVT";
    static final String DEPOSIT_RECEIVER_PHONE = "0914.339.682; 0846.557.999";

    RoomRepository roomRepository;
    PropertyRepository propertyRepository;
    DepositFormRepository depositFormRepository;
    UploadFileUseCase uploadFileUseCase;
    DownloadFileUseCase downloadFileUseCase;
    DefaultConfig defaultConfig;
    DepositAgreementRepository depositAgreementRepository;
    PlatformTransactionManager transactionManager;
    TemplateEngine templateEngine;
    Environment environment;

    @Transactional(readOnly = true)
    public DepositContractPreviewResponse preview(DepositContractPreviewRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        Property property = propertyRepository.findById(room.getPropertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        ContractData data = ContractData.fromPreview(
                request,
                room,
                property,
                ownerInfo(),
                resolvePreviewDepositAmount(),
                LocalDateTime.now()
        );
        return DepositContractPreviewResponse.builder()
                .html(buildTemplateHtml(data))
                .depositCode(data.depositCode())
                .depositAmount(data.depositAmount())
                .depositAmountText(data.depositAmountText())
                .generatedAt(formatDateTime(data.generatedAt()))
                .build();
    }

    private Long resolvePreviewDepositAmount() {
        Long amount = environment == null
                ? DEFAULT_DEPOSIT_AMOUNT
                : environment.getProperty("app.deposit.amount", Long.class, DEFAULT_DEPOSIT_AMOUNT);
        if (amount == null) {
            return DEFAULT_DEPOSIT_AMOUNT;
        }
        return amount <= 0 ? DEFAULT_DEPOSIT_AMOUNT : amount;
    }

    @Transactional(readOnly = true)
    public DepositContractPreviewResponse previewDraft(Long depositAgreementId) {
        DepositAgreement agreement = depositAgreementRepository.findById(depositAgreementId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        ContractData data = buildOfficialData(agreement);
        return DepositContractPreviewResponse.builder()
                .html(buildTemplateHtml(data))
                .depositCode(data.depositCode())
                .depositAmount(data.depositAmount())
                .depositAmountText(data.depositAmountText())
                .generatedAt(formatDateTime(data.generatedAt()))
                .build();
    }

    @Transactional
    public Long ensureOfficialContractFile(Long depositAgreementId) {
        return ensureOfficialContractFileInCurrentTransaction(depositAgreementId);
    }

    public void generateOfficialContractAfterCommit(Long depositAgreementId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    generateOfficialContractSafely(depositAgreementId);
                }
            });
            return;
        }

        generateOfficialContractSafely(depositAgreementId);
    }

    public void regenerateOfficialContractAfterCommit(Long depositAgreementId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    regenerateOfficialContractSafely(depositAgreementId);
                }
            });
            return;
        }

        regenerateOfficialContractSafely(depositAgreementId);
    }

    private void regenerateOfficialContractSafely(Long depositAgreementId) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.executeWithoutResult(status -> regenerateOfficialContractFileInCurrentTransaction(depositAgreementId));
        } catch (RuntimeException ex) {
            log.warn("Could not regenerate deposit contract PDF. depositAgreementId={}", depositAgreementId, ex);
        }
    }

    private void generateOfficialContractSafely(Long depositAgreementId) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.executeWithoutResult(status -> ensureOfficialContractFileInCurrentTransaction(depositAgreementId));
        } catch (RuntimeException ex) {
            log.warn("Could not generate deposit contract PDF. depositAgreementId={}", depositAgreementId, ex);
        }
    }

    private Long ensureOfficialContractFileInCurrentTransaction(Long depositAgreementId) {
        DepositAgreement agreement = depositAgreementRepository.findById(depositAgreementId)
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        if (agreement.getContractFileId() != null) {
            return agreement.getContractFileId();
        }

        return createAndAttachOfficialContract(agreement);
    }

    private Long regenerateOfficialContractFileInCurrentTransaction(Long depositAgreementId) {
        DepositAgreement agreement = depositAgreementRepository.findById(depositAgreementId)
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        return createAndAttachOfficialContract(agreement);
    }

    private Long createAndAttachOfficialContract(DepositAgreement agreement) {
        ContractData data = buildOfficialData(agreement);
        byte[] pdfBytes = generatePdf(data);
        String filename = "hop-dong-dat-coc-" + safeFilename(data.depositCode()) + ".pdf";
        var uploadCommand = new UploadFileCommand(
                null,
                new ByteArrayMultipartFile("file", filename, "application/pdf", pdfBytes),
                FileCategory.DEPOSIT_CONTRACT,
                true
        );
        var uploaded = uploadContract(uploadCommand);

        agreement.attachContractFile(uploaded.getId());
        depositAgreementRepository.save(agreement);
        return uploaded.getId();
    }

    private com.sep490.hdbhms.file.domain.model.FileMetadata uploadContract(UploadFileCommand command) {
        try {
            return uploadFileUseCase.execute(command);
        } catch (IOException e) {
            throw new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND);
        }
    }

    @Transactional
    public FileDataResponse getOfficialContractFile(Long depositAgreementId) {
        Long fileId = ensureOfficialContractFile(depositAgreementId);
        FileDataResponse response = downloadFileUseCase.execute(new DownloadFileQuery(fileId));
        if (response == null) {
            throw new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND);
        }
        return response;
    }

    private ContractData buildOfficialData(DepositAgreement agreement) {
        Room room = roomRepository.findById(agreement.getRoomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        Property property = propertyRepository.findById(room.getPropertyId())
                .orElseThrow(() -> new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND));
        DepositForm form = agreement.getDepositFormId() == null
                ? null
                : depositFormRepository.findById(agreement.getDepositFormId()).orElse(null);
        return ContractData.fromAgreement(
                agreement,
                form,
                room,
                property,
                ownerInfo(),
                agreement.getConfirmedAt() != null ? agreement.getConfirmedAt() : LocalDateTime.now()
        );
    }

    private OwnerInfo ownerInfo() {
        return new OwnerInfo(
                DEPOSIT_RECEIVER_FULL_NAME,
                DEPOSIT_RECEIVER_DOB,
                DEPOSIT_RECEIVER_ID_NUMBER,
                DEPOSIT_RECEIVER_ID_ISSUED_DATE,
                DEPOSIT_RECEIVER_ID_ISSUED_PLACE,
                DEPOSIT_RECEIVER_PHONE
        );
    }

    private String buildTemplateHtml(ContractData data) {
        Context context = new Context();
        context.setVariables(buildTemplateVariables(data));
        return templateEngine.process("contractTemplates/html/deposit_contract_template", context);
    }

    private Map<String, Object> buildTemplateVariables(ContractData data) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("draftNotice", "BẢN NHÁP - CHƯA CÓ CHỮ KÝ");
        variables.put("draftDescription", "Bản dùng để in và ký trực tiếp, chưa phải bản hợp đồng đặt cọc chính thức.");
        variables.put("issuedAtDate", formatDate(data.generatedAt().toLocalDate()));
        variables.put("issuedAtDateString", formatVietnameseDate(data.generatedAt().toLocalDate()));
        variables.put("ownerFullNameUppercase", data.owner().fullName().toUpperCase());
        variables.put("ownerDob", data.owner().dob());
        variables.put("ownerIdNumber", data.owner().idNumber());
        variables.put("ownerIdIssuedDate", data.owner().idIssuedDate());
        variables.put("ownerIdIssuedPlace", data.owner().idIssuedPlace());
        variables.put("ownerContactPhoneListString", data.owner().phone());
        variables.put("signerFullName", data.customerName());
        variables.put("signerDob", formatDate(data.customerDob()));
        variables.put("dob", formatDate(data.customerDob()));
        variables.put("signerIdNumber", valueOrDefault(data.idNumber(), "............"));
        variables.put("signerIdIssuedDate", formatDate(data.idIssueDate()));
        variables.put("signerIdIssuedPlace", valueOrDefault(data.idIssuePlace(), "............"));
        variables.put("signerPhoneNumber", data.customerPhone());
        variables.put("signerPermanentAddress", valueOrDefault(data.permanentAddress(), "............"));
        variables.put("roomNumber", data.roomCode());
        variables.put("propertyAddress", valueOrDefault(data.propertyAddress(), "............"));
        variables.put("signerNumberOfOccupants", data.maxOccupants() == null ? "............" : data.maxOccupants().toString());
        variables.put("expectedMoveInDateString", formatVietnameseDate(data.expectedMoveInDate()));
        variables.put("listedPrice", formatMoney(data.listedPrice()));
        variables.put("expectedLeaseSignDate", formatDate(data.expectedLeaseSignDate()));
        variables.put("contractDuration", "12");
        variables.put("contractStartDate", formatVietnameseDate(data.expectedMoveInDate()));
        variables.put("depositAmount", formatMoney(data.depositAmount()));
        variables.put("depositAmountString", data.depositAmountText());
        variables.put("depositSignedDateString", formatVietnameseDate(data.generatedAt().toLocalDate()));
        variables.put("currentYear", data.generatedAt().getYear());
        variables.put("paymentCycleMonths", data.paymentCycleMonths() != null ? data.paymentCycleMonths() : 1);
        variables.put("depositMonths", data.depositMonths() != null ? data.depositMonths() : 1);
        return variables;
    }

    //    private byte[] generatePdf(ContractData data) {
//        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
//            FontRef fontRef = loadFont(document);
//            PdfWriter writer = new PdfWriter(document, fontRef);
//            writer.center("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", 13);
//            writer.center("Độc Lập – Tự Do – Hạnh Phúc", 12);
//            writer.center("--------o0o--------", 12);
//            writer.gap(16);
//            writer.center("HỢP ĐỒNG ĐẶT CỌC TIỀN PHÒNG", 17);
//            writer.center("BAN NHAP - CHUA CO CHU KY", 11);
//            writer.center("Ban dung de in va ky truc tiep, chua phai ban chinh thuc.", 10);
//            writer.center("Mã cọc: " + data.depositCode(), 11);
//            writer.gap(14);
//            writer.paragraph("Hôm nay ngày " + formatDate(data.generatedAt().toLocalDate()), 11);
//            writer.paragraph("Chúng tôi những người ký tên dưới đây gồm:", 11);
//            writer.section("1. BÊN NHẬN TIỀN ĐẶT CỌC (BÊN A)");
//            writer.paragraph("Ông: " + data.owner().fullName(), 11);
//            writer.paragraph("Ngày sinh: " + data.owner().dob(), 11);
//            writer.paragraph("CMTND/CCCD Số: " + data.owner().idNumber()
//                    + "; Cấp ngày: " + data.owner().idIssuedDate()
//                    + "  Nơi cấp: " + data.owner().idIssuedPlace(), 11);
//            writer.paragraph("Điện thoại: " + data.owner().phone(), 11);
//            writer.paragraph("Là chủ sở hữu và sử dụng hợp pháp của toàn bộ căn nhà nêu tại Điều 1 dưới đây.", 11);
//            writer.section("2. BÊN GIAO TIỀN ĐẶT CỌC (BÊN B)");
//            writer.paragraph("Ông/ Bà: " + data.customerName() + "    Ngày sinh: " + formatDate(data.customerDob()), 11);
//            writer.paragraph("CMTND/ CCCD: " + valueOrDefault(data.idNumber(), "Chưa cung cấp")
//                    + "    Cấp ngày: " + formatDate(data.idIssueDate())
//                    + "    Nơi cấp: " + valueOrDefault(data.idIssuePlace(), "Chưa cung cấp"), 11);
//            writer.paragraph("Điện thoại: " + data.customerPhone(), 11);
//            writer.paragraph("Địa chỉ thường trú: " + valueOrDefault(data.permanentAddress(), "Chưa cung cấp"), 11);
//            writer.paragraph("Sau khi bàn bạc, hai bên cùng đi đến thống nhất 1 số điều khoản như sau:", 11);
//            writer.section("ĐIỀU 1: TIỀN ĐẶT CỌC, MỤC ĐÍCH ĐẶT CỌC");
//            writer.paragraph("1.1 Bên B đồng ý thuê của bên A phòng số " + data.roomCode()
//                    + ". Tòa nhà có địa chỉ: " + valueOrDefault(data.propertyAddress(), "Chưa cung cấp")
//                    + ". Do bên A đại diện là Ban Quản Lý tòa nhà với các thỏa thuận trong hợp đồng như sau:", 11);
//            writer.paragraph("Mục đích thuê: để ở và sinh hoạt với số lượng người đăng kí ở: "
//                    + (data.maxOccupants() == null ? "............" : data.maxOccupants()), 11);
//            writer.paragraph("Hợp đồng có thời hạn: ... tháng và bắt đầu tính tiền từ ngày .., tháng ..., năm "
//                    + data.generatedAt().getYear() + ".", 11);
//            writer.paragraph("Giá thuê phòng: " + formatMoney(data.listedPrice()) + " / 01 tháng", 11);
//            writer.paragraph("Phương thức thanh toán: Đặt cọc ………. tháng và thanh toán ……………. tháng tiền nhà", 11);
//            writer.paragraph("1.2 Để đảm bảo chắc chắn việc ký hợp đồng thuê phòng và trả tiền thuê phòng muộn nhất vào "
//                    + formatDate(data.expectedLeaseSignDate()) + ", nay bên B tự nguyện đóng cho bên A số tiền là "
//                    + formatMoney(data.depositAmount()) + " (Bằng chữ: " + data.depositAmountText()
//                    + ") gọi là tiền đặt cọc.", 11);
//            writer.section("ĐIỀU 2: THỎA THUẬN VỀ VIỆC GIẢI QUYẾT TIỀN ĐẶT CỌC");
//            writer.paragraph("2.1 Từ ngày ký hợp đồng này đến ngày " + formatDate(data.expectedLeaseSignDate())
//                    + " mà bên B không liên hệ để ký hợp đồng thuê phòng và trả tiền thuê phòng thì bên B sẽ mất toàn bộ số tiền đã đặt cọc.", 11);
//            writer.paragraph("2.2 Nếu đến hết ngày " + formatDate(data.expectedLeaseSignDate())
//                    + " mà bên A không ký hợp đồng cho thuê với bên B thì bên A phải trả lại cho bên B toàn bộ số tiền mà bên B đã đặt cọc.", 11);
//            writer.section("ĐIỀU 3: CAM KẾT CỦA HAI BÊN");
//            writer.paragraph("3.1 Hai bên xác định hoàn toàn tự nguyện khi ký hợp đồng này và cam kết cùng nhau thực hiện nghiêm túc những điều khoản trên đây.", 11);
//            writer.paragraph("3.2 Hợp đồng này có hiệu lực từ ngày ký, hợp đồng được lập thành 02 bản, mỗi bên giữ 01 bản có giá trị pháp lý như nhau. Sau khi đọc hợp đồng cả 2 bên đã hiểu rõ quyền lợi và nghĩa vụ của mình và cùng ký tên dưới đây.", 11);
//            writer.gap(10);
//            writer.paragraph("Hà Nội, " + formatVietnameseDate(data.generatedAt().toLocalDate()), 11);
//            writer.signatures(data.owner().fullName(), data.customerName());
//            writer.close();
//            document.save(output);
//            return output.toByteArray();
//        } catch (IOException e) {
//            throw new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND);
//        }
//    }
    private byte[] generatePdf(ContractData data) {
        String html = buildTemplateHtml(data);
        return renderHtmlToPdf(html);
    }

    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            ITextFontResolver fontResolver = renderer.getFontResolver();

            ClassLoader cl = getClass().getClassLoader();
            for (String f : List.of(
                    "fonts/times.ttf",
                    "fonts/timesbd.ttf",
                    "fonts/timesi.ttf",
                    "fonts/timesbi.ttf"
            )) {
                URL fontUrl = cl.getResource(f);
                if (fontUrl != null) {
                    fontResolver.addFont(fontUrl.toExternalForm(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            }

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new AppException(ApiErrorCode.DEPOSIT_AGREEMENT_NOT_FOUND);
        }
    }

    private FontRef loadFont(PDDocument document) throws IOException {
        List<String> candidates = List.of(
                "C:/Windows/Fonts/times.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        );
        for (String candidate : candidates) {
            File file = new File(candidate);
            if (file.exists()) {
                return new FontRef(PDType0Font.load(document, file), false);
            }
        }
        return new FontRef(new PDType1Font(Standard14Fonts.FontName.HELVETICA), true);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "Chưa cung cấp" : DATE_FORMATTER.format(date);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "Chưa cung cấp" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatVietnameseDate(LocalDate date) {
        if (date == null) {
            return "............";
        }
        return "ngày %d, tháng %d, năm %d".formatted(
                date.getDayOfMonth(),
                date.getMonthValue(),
                date.getYear()
        );
    }

    private String formatMoney(Long amount) {
        long safeAmount = amount == null ? 0L : amount;
        return MONEY_FORMATTER.format(safeAmount) + " VND";
    }

    private String amountText(Long amount) {
        long safeAmount = amount == null ? 0L : amount;
        String text = com.sep490.hdbhms.shared.utils.StringUtils.toVietnamesePriceString(safeAmount);
        if (text.isBlank()) {
            return "không đồng";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1) + " đồng";
    }

    private String safeFilename(String value) {
        String normalized = Normalizer.normalize(valueOrDefault(value, "deposit-contract"), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .replaceAll("[^a-zA-Z0-9._-]+", "-")
                .replaceAll("^-|-$", "")
                .toLowerCase(Locale.ROOT);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String stripUnsupportedControlChars(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.getType(ch) == Character.CONTROL && ch != '\r' && ch != '\n' && ch != '\t') {
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private String ascii(String value) {
        String normalized = Normalizer.normalize(valueOrDefault(value, ""), Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private record OwnerInfo(
            String fullName,
            String dob,
            String idNumber,
            String idIssuedDate,
            String idIssuedPlace,
            String phone
    ) {
    }

    private record FontRef(PDFont font, boolean asciiOnly) {
        String text(String value) {
            String cleanValue = stripUnsupportedControlChars(valueOrEmpty(value));
            if (!asciiOnly) {
                return cleanValue;
            }
            String normalized = Normalizer.normalize(cleanValue, Normalizer.Form.NFD);
            return DIACRITICS.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
        }

        private static String valueOrEmpty(String value) {
            return value == null ? "" : value;
        }
    }

    private record ContractData(
            String depositCode,
            String customerName,
            LocalDate customerDob,
            String idNumber,
            LocalDate idIssueDate,
            String idIssuePlace,
            String customerPhone,
            String customerEmail,
            String permanentAddress,
            String roomCode,
            String propertyName,
            String propertyAddress,
            Long depositAmount,
            String depositAmountText,
            Long listedPrice,
            Integer maxOccupants,
            LocalDate expectedLeaseSignDate,
            LocalDate expectedMoveInDate,
            LocalDateTime generatedAt,
            Integer paymentCycleMonths,
            Integer depositMonths,
            OwnerInfo owner
    ) {
        static ContractData fromPreview(
                DepositContractPreviewRequest request,
                Room room,
                Property property,
                OwnerInfo owner,
                Long depositAmount,
                LocalDateTime generatedAt
        ) {
            return new ContractData(
                    "Bản nháp",
                    request.getFullName(),
                    request.getDob(),
                    request.getIdNumber(),
                    request.getIdIssueDate(),
                    request.getIdIssuePlace(),
                    request.getPhone(),
                    request.getEmail(),
                    request.getPermanentAddress(),
                    room.getRoomCode(),
                    property.getName(),
                    property.getAddressLine(),
                    depositAmount,
                    textAmount(depositAmount),
                    room.getListedPrice(),
                    room.getMaxOccupants(),
                    request.getExpectedLeaseSignDate(),
                    request.getExpectedMoveInDate(),
                    generatedAt,
                    request.getPaymentCycleMonths(),
                    1,
                    owner
            );
        }

        static ContractData fromAgreement(
                DepositAgreement agreement,
                DepositForm form,
                Room room,
                Property property,
                OwnerInfo owner,
                LocalDateTime generatedAt
        ) {
            return new ContractData(
                    agreement.getDepositCode(),
                    form != null ? form.getFullName() : "Khách đặt cọc",
                    form != null ? form.getDob() : null,
                    form != null ? form.getIdNumber() : null,
                    form != null ? form.getIdIssueDate() : null,
                    form != null ? form.getIdIssuePlace() : null,
                    form != null ? form.getPhone() : "Chưa cung cấp",
                    form != null ? form.getEmail() : null,
                    form != null ? form.getPermanentAddress() : null,
                    room.getRoomCode(),
                    property.getName(),
                    property.getAddressLine(),
                    agreement.getAmount(),
                    textAmount(agreement.getAmount()),
                    room.getListedPrice(),
                    room.getMaxOccupants(),
                    agreement.getExpectedLeaseSignDate(),
                    agreement.getExpectedMoveInDate(),
                    generatedAt,
                    form != null && form.getPaymentCycleMonths() != null ? form.getPaymentCycleMonths() : 1,
                    form != null && form.getDepositMonths() != null ? form.getDepositMonths() : 1,
                    owner
            );
        }

        private static String textAmount(Long amount) {
            long safeAmount = amount == null ? 0L : amount;
            String text = com.sep490.hdbhms.shared.utils.StringUtils.toVietnamesePriceString(safeAmount);
            return text.isBlank()
                    ? "Không đồng"
                    : Character.toUpperCase(text.charAt(0)) + text.substring(1) + " đồng";
        }
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes == null ? new byte[0] : bytes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }

    private final class PdfWriter {
        private static final float MARGIN_X = 56;
        private static final float MARGIN_TOP = 54;
        private static final float MARGIN_BOTTOM = 54;
        private final PDDocument document;
        private final FontRef fontRef;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private PdfWriter(PDDocument document, FontRef fontRef) throws IOException {
            this.document = document;
            this.fontRef = fontRef;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN_TOP;
        }

        private void center(String text, float size) throws IOException {
            ensureSpace(size + 8);
            String safeText = fontRef.text(text);
            float width = fontRef.font().getStringWidth(safeText) / 1000 * size;
            float x = (page.getMediaBox().getWidth() - width) / 2;
            draw(safeText, x, y, size);
            y -= size + 6;
        }

        private void section(String text) throws IOException {
            gap(8);
            paragraph(text, 12);
        }

        private void paragraph(String text, float size) throws IOException {
            List<String> lines = wrap(fontRef.text(text), size, page.getMediaBox().getWidth() - (MARGIN_X * 2));
            for (String line : lines) {
                ensureSpace(size + 6);
                draw(line, MARGIN_X, y, size);
                y -= size + 6;
            }
        }

        private void signatures(String ownerName, String customerName) throws IOException {
            gap(28);
            ensureSpace(140);
            float pageWidth = page.getMediaBox().getWidth();
            float boxWidth = (pageWidth - MARGIN_X * 2 - 36) / 2;
            float leftX = MARGIN_X;
            float rightX = MARGIN_X + boxWidth + 36;
            drawCenteredInBox("BÊN NHẬN CỌC", leftX, boxWidth, y, 11);
            drawCenteredInBox("BÊN ĐẶT CỌC", rightX, boxWidth, y, 11);
            y -= 18;
            drawCenteredInBox("Ký, ghi rõ họ tên", leftX, boxWidth, y, 10);
            drawCenteredInBox("Ký, ghi rõ họ tên", rightX, boxWidth, y, 10);
            y -= 88;
            drawCenteredInBox(ownerName, leftX, boxWidth, y, 11);
            drawCenteredInBox(customerName, rightX, boxWidth, y, 11);
            y -= 18;
        }

        private void drawCenteredInBox(String text, float x, float boxWidth, float y, float size) throws IOException {
            String safeText = fontRef.text(text);
            float width = fontRef.font().getStringWidth(safeText) / 1000 * size;
            draw(safeText, x + (boxWidth - width) / 2, y, size);
        }

        private void gap(float gap) throws IOException {
            ensureSpace(gap);
            y -= gap;
        }

        private void ensureSpace(float height) throws IOException {
            if (y - height < MARGIN_BOTTOM) {
                newPage();
            }
        }

        private void draw(String text, float x, float y, float size) throws IOException {
            stream.beginText();
            stream.setFont(fontRef.font(), size);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }

        private List<String> wrap(String text, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = fontRef.font().getStringWidth(candidate) / 1000 * size;
                if (width <= maxWidth) {
                    current = new StringBuilder(candidate);
                } else {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                    }
                    current = new StringBuilder(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
