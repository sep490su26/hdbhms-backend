package com.sep490.hdbhms.occupancy.application.service;

import com.lowagie.text.pdf.BaseFont;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractDocumentService {
    static final java.time.format.DateTimeFormatter DATE_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final NumberFormat MONEY_FORMATTER = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    static final String OWNER_FULL_NAME = "\u0110\u1eb6NG V\u0102N NHU\u1ea6N";
    static final String OWNER_DOB = "06/08/1978";
    static final String OWNER_ID_NUMBER = "036078008683";
    static final String OWNER_ID_ISSUED_DATE = "01/04/2020";
    static final String OWNER_ID_ISSUED_PLACE = "C\u1ee5c c\u1ea3nh s\u00e1t QLHCVT";
    static final String OWNER_PHONE = "0914.339.682; 0846.557.999";
    static final String OWNER_BANK_ACCOUNT = ".............";
    static final String OWNER_BANK_NAME = ".............";
    static final long DEPOSIT_AMOUNT = 2_000L;

    TemplateEngine templateEngine;
    JdbcTemplate jdbcTemplate;

    public byte[] generateDraftPdf(Long leaseContractId) {
        return renderHtmlToPdf(buildLeaseTemplateHtml(fetchContractData(leaseContractId)));
    }

    public String previewDepositContract(Map<String, Object> input) {
        Long roomId = toLong(input.get("roomId"));
        if (roomId == null) {
            throw new AppException(ApiErrorCode.ROOM_NOT_FOUND);
        }
        LocalDate today = LocalDate.now();
        LocalDate dob = toDate(input.get("dob"));
        LocalDate expectedMoveInDate = toDate(input.get("expectedMoveInDate"));
        LocalDate expectedLeaseSignDate = toDate(input.get("expectedLeaseSignDate"));
        if (dob != null && dob.isAfter(today)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (expectedMoveInDate != null && expectedMoveInDate.isBefore(today)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        if (expectedLeaseSignDate != null && expectedLeaseSignDate.isBefore(today)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        ContractTemplateData data = jdbcTemplate.query(
                """
                SELECT r.room_code, r.listed_price, p.name AS property_name, p.address_line AS property_address
                FROM rooms r
                JOIN properties p ON p.property_id = r.property_id
                WHERE r.room_id = ?
                """,
                rs -> rs.next() ? previewData(rs, input) : null,
                roomId
        );
        if (data == null) {
            throw new AppException(ApiErrorCode.ROOM_NOT_FOUND);
        }
        return buildLeaseTemplateHtml(data);
    }

    private ContractTemplateData fetchContractData(Long contractId) {
        String sql = """
            SELECT
                c.contract_code,
                c.deposit_form_id,
                c.start_date,
                c.end_date,
                c.rent_start_date,
                c.monthly_rent,
                c.payment_cycle_months,
                c.deposit_amount,
                r.room_code,
                r.listed_price,
                p.name AS property_name,
                p.address_line AS property_address,
                u1.unit_price AS electricity_price,
                u3.unit_price AS utility_price,
                pp.full_name AS tenant_name,
                pp.dob AS tenant_dob,
                pp.phone AS tenant_phone,
                pp.permanent_address AS tenant_address,
                ide.doc_number AS tenant_id_number,
                ide.issued_date AS tenant_id_issued_date,
                ide.issued_place AS tenant_id_issued_place
            FROM lease_contracts c
            JOIN rooms r ON c.room_id = r.room_id
            JOIN properties p ON r.property_id = p.property_id
            JOIN person_profiles pp ON c.primary_tenant_profile_id = pp.person_profile_id
            LEFT JOIN identity_documents ide
                ON pp.person_profile_id = ide.profile_id
                AND ide.status = 'ACTIVE'
                AND ide.doc_type = 'CCCD'
            LEFT JOIN utility_tariffs u1
                ON u1.property_id = p.property_id
                AND u1.utility_type = 'ELECTRICITY'
                AND (u1.effective_to IS NULL OR u1.effective_to >= CURRENT_DATE)
            LEFT JOIN utility_tariffs u3
                ON u3.property_id = p.property_id
                AND u3.utility_type = 'SERVICE_FEE'
                AND (u3.effective_to IS NULL OR u3.effective_to >= CURRENT_DATE)
            WHERE c.lease_contract_id = ?
            """;

        List<ContractTemplateData> results = jdbcTemplate.query(sql, this::mapRowToData, contractId);
        if (results.isEmpty()) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        ContractTemplateData data = results.get(0);
        data.setOccupants(fetchContractOccupants(contractId, data.getDepositFormId(), data));
        return data;
    }

    private ContractTemplateData mapRowToData(ResultSet rs, int rowNum) throws SQLException {
        LocalDate startDate = localDate(rs, "start_date");
        LocalDate endDate = localDate(rs, "end_date");
        int termMonths = startDate == null || endDate == null
                ? 12
                : (int) Math.max(6, ChronoUnit.MONTHS.between(startDate, endDate.plusDays(1)));
        return ContractTemplateData.builder()
                .contractCode(rs.getString("contract_code"))
                .depositFormId(rs.getObject("deposit_form_id", Long.class))
                .startDate(startDate)
                .endDate(endDate)
                .rentStartDate(localDate(rs, "rent_start_date"))
                .monthlyRent(rs.getObject("monthly_rent", Long.class))
                .paymentCycleMonths(rs.getObject("payment_cycle_months", Integer.class))
                .depositAmount(rs.getObject("deposit_amount", Long.class))
                .roomCode(rs.getString("room_code"))
                .roomListedPrice(rs.getObject("listed_price", Long.class))
                .propertyName(rs.getString("property_name"))
                .propertyAddress(rs.getString("property_address"))
                .electricityPrice(rs.getObject("electricity_price", Long.class))
                .utilityPrice(rs.getObject("utility_price", Long.class))
                .tenantName(rs.getString("tenant_name"))
                .tenantDob(localDate(rs, "tenant_dob"))
                .tenantPhone(rs.getString("tenant_phone"))
                .tenantAddress(rs.getString("tenant_address"))
                .tenantIdNumber(rs.getString("tenant_id_number"))
                .tenantIdIssuedDate(localDate(rs, "tenant_id_issued_date"))
                .tenantIdIssuedPlace(rs.getString("tenant_id_issued_place"))
                .leaseTermMonths(termMonths)
                .renewalTermMonths(termMonths)
                .handoverDate(startDate)
                .handoverElectricityMeter("............")
                .otherFees("............")
                .build();
    }

    private ContractTemplateData previewData(ResultSet rs, Map<String, Object> input) throws SQLException {
        LocalDate startDate = toDate(input.get("expectedMoveInDate"));
        LocalDate signDate = toDate(input.get("expectedLeaseSignDate"));
        int termMonths = Math.max(6, toInt(input.get("contractTermMonths"), 12));
        int paymentCycleMonths = toInt(input.get("paymentCycleMonths"), 1);
        long monthlyRent = valueOrZero(rs.getObject("listed_price", Long.class));
        long depositAmount = DEPOSIT_AMOUNT;
        ContractTemplateData data = ContractTemplateData.builder()
                .contractCode("DRAFT-" + valueOrDefault(rs.getString("room_code"), "ROOM"))
                .startDate(startDate)
                .endDate(startDate == null ? null : startDate.plusMonths(termMonths).minusDays(1))
                .rentStartDate(startDate)
                .monthlyRent(monthlyRent)
                .paymentCycleMonths(paymentCycleMonths)
                .depositAmount(depositAmount)
                .roomCode(rs.getString("room_code"))
                .roomListedPrice(monthlyRent)
                .propertyName(rs.getString("property_name"))
                .propertyAddress(rs.getString("property_address"))
                .tenantName(stringValue(input.get("fullName")))
                .tenantDob(toDate(input.get("dob")))
                .tenantPhone(stringValue(input.get("phone")))
                .tenantAddress(stringValue(input.get("permanentAddress")))
                .tenantIdNumber(stringValue(input.get("idNumber")))
                .tenantIdIssuedDate(toDate(input.get("idIssueDate")))
                .tenantIdIssuedPlace(stringValue(input.get("idIssuePlace")))
                .electricityPrice(null)
                .utilityPrice(null)
                .leaseTermMonths(termMonths)
                .renewalTermMonths(termMonths)
                .handoverDate(startDate)
                .handoverElectricityMeter("............")
                .otherFees("............")
                .build();
        data.setOccupants(previewOccupants(input, data));
        data.setIssuedAtDate(signDate == null ? LocalDate.now() : signDate);
        return data;
    }

    private List<TemplateOccupant> fetchContractOccupants(Long contractId, Long depositFormId, ContractTemplateData data) {
        List<TemplateOccupant> occupants = jdbcTemplate.query(
                """
                SELECT pp.full_name, pp.phone, ide.doc_number, ide.issued_date, ide.issued_place
                FROM contract_occupants co
                JOIN person_profiles pp ON pp.person_profile_id = co.tenant_profile_id
                LEFT JOIN identity_documents ide
                    ON ide.profile_id = pp.person_profile_id AND ide.status = 'ACTIVE' AND ide.doc_type = 'CCCD'
                WHERE co.contract_id = ? AND co.status = 'ACTIVE'
                ORDER BY CASE WHEN co.occupant_role = 'PRIMARY' THEN 0 ELSE 1 END, co.contract_occupant_id
                """,
                (rs, rowNum) -> TemplateOccupant.builder()
                        .fullName(rs.getString("full_name"))
                        .phone(rs.getString("phone"))
                        .idNumber(rs.getString("doc_number"))
                        .idIssuedDate(formatDate(localDate(rs, "issued_date")))
                        .idIssuedPlace(rs.getString("issued_place"))
                        .hometown("............")
                        .build(),
                contractId
        );
        if (!occupants.isEmpty() || depositFormId == null) {
            return occupants;
        }
        return previewOccupantsFromDepositForm(depositFormId, data);
    }

    private List<TemplateOccupant> previewOccupants(Map<String, Object> input, ContractTemplateData data) {
        List<TemplateOccupant> occupants = new ArrayList<>();
        occupants.add(primaryOccupant(data));
        Object raw = input.get("coOccupants");
        if (raw instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof Map<?, ?> map) {
                    occupants.add(TemplateOccupant.builder()
                            .fullName(stringValue(map.get("fullName")))
                            .phone(stringValue(map.get("phone")))
                            .idNumber("............")
                            .idIssuedDate("............")
                            .idIssuedPlace("............")
                            .hometown("............")
                            .build());
                }
            }
        }
        return occupants;
    }

    private List<TemplateOccupant> previewOccupantsFromDepositForm(Long depositFormId, ContractTemplateData data) {
        List<TemplateOccupant> occupants = new ArrayList<>();
        occupants.add(primaryOccupant(data));
        occupants.addAll(jdbcTemplate.query(
                "SELECT full_name, phone FROM deposit_form_co_occupants WHERE deposit_form_id = ? ORDER BY display_order",
                (rs, rowNum) -> TemplateOccupant.builder()
                        .fullName(rs.getString("full_name"))
                        .phone(rs.getString("phone"))
                        .idNumber("............")
                        .idIssuedDate("............")
                        .idIssuedPlace("............")
                        .hometown("............")
                        .build(),
                depositFormId
        ));
        return occupants;
    }

    private TemplateOccupant primaryOccupant(ContractTemplateData data) {
        return TemplateOccupant.builder()
                .fullName(valueOrDefault(data.getTenantName(), "............"))
                .phone(valueOrDefault(data.getTenantPhone(), "............"))
                .idNumber(valueOrDefault(data.getTenantIdNumber(), "............"))
                .idIssuedDate(formatDate(data.getTenantIdIssuedDate()))
                .idIssuedPlace(valueOrDefault(data.getTenantIdIssuedPlace(), "............"))
                .hometown("............")
                .build();
    }

    private String buildLeaseTemplateHtml(ContractTemplateData data) {
        Context context = new Context();
        context.setVariables(buildLeaseVariables(data));
        return templateEngine.process("contractTemplates/html/lease_contract_template", context);
    }

    private Map<String, Object> buildLeaseVariables(ContractTemplateData data) {
        Map<String, Object> variables = new HashMap<>();
        LocalDate issuedAtDate = data.getIssuedAtDate() == null
                ? (data.getStartDate() == null ? LocalDate.now() : data.getStartDate())
                : data.getIssuedAtDate();
        variables.put("issuedAtDateString", formatDate(issuedAtDate));
        variables.put("issuedAtPlace", valueOrDefault(data.getIssuedAtPlace(), valueOrDefault(data.getPropertyAddress(), "............")));
        variables.put("contractCode", valueOrDefault(data.getContractCode(), "............"));

        variables.put("ownerFullNameUppercase", OWNER_FULL_NAME);
        variables.put("ownerDob", OWNER_DOB);
        variables.put("ownerIdNumber", OWNER_ID_NUMBER);
        variables.put("ownerIdIssuedDate", OWNER_ID_ISSUED_DATE);
        variables.put("ownerIdIssuedPlace", OWNER_ID_ISSUED_PLACE);
        variables.put("ownerPermanentAddress", "............");
        variables.put("ownerHometown", "............");

        variables.put("signerFullName", valueOrDefault(data.getTenantName(), "............"));
        variables.put("signerDob", formatDate(data.getTenantDob()));
        variables.put("signerPermanentAddress", valueOrDefault(data.getTenantAddress(), "............"));
        variables.put("signerIdNumber", valueOrDefault(data.getTenantIdNumber(), "............"));
        variables.put("signerIdIssuedDate", formatDate(data.getTenantIdIssuedDate()));
        variables.put("signerIdIssuedPlace", valueOrDefault(data.getTenantIdIssuedPlace(), "............"));
        variables.put("signerPhoneNumber", valueOrDefault(data.getTenantPhone(), "............"));
        variables.put("signerNumberOfOccupants", data.getOccupants() == null || data.getOccupants().isEmpty() ? 1 : data.getOccupants().size());
        variables.put("occupants", data.getOccupants() == null ? List.of(primaryOccupant(data)) : data.getOccupants());

        variables.put("roomCode", valueOrDefault(data.getRoomCode(), "............"));
        variables.put("propertyName", valueOrDefault(data.getPropertyName(), "............"));
        variables.put("propertyAddress", valueOrDefault(data.getPropertyAddress(), "............"));
        variables.put("handoverDateString", formatDate(data.getHandoverDate()));
        variables.put("handoverElectricityMeter", valueOrDefault(data.getHandoverElectricityMeter(), "............"));

        variables.put("leaseTermMonths", data.getLeaseTermMonths() == null ? 12 : data.getLeaseTermMonths());
        variables.put("renewalTermMonths", data.getRenewalTermMonths() == null ? 12 : data.getRenewalTermMonths());
        variables.put("leaseContractStartDateString", formatDate(data.getStartDate()));
        variables.put("rentStartDateString", formatDate(data.getRentStartDate()));
        variables.put("monthlyRent", formatMoney(data.getMonthlyRent()));
        variables.put("monthlyRentString", amountText(data.getMonthlyRent()));
        variables.put("paymentCycleMonths", data.getPaymentCycleMonths() == null ? 1 : data.getPaymentCycleMonths());
        variables.put("depositAmount", formatMoney(data.getDepositAmount()));
        variables.put("depositAmountString", amountText(data.getDepositAmount()));
        variables.put("propertyElectricityPrice", formatMoney(data.getElectricityPrice()));
        variables.put("propertyUtilityPrice", formatMoney(data.getUtilityPrice()));
        variables.put("otherFees", valueOrDefault(data.getOtherFees(), "............"));
        return variables;
    }

    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            ITextFontResolver fontResolver = renderer.getFontResolver();
            ClassLoader classLoader = getClass().getClassLoader();
            for (String font : List.of("fonts/times.ttf", "fonts/timesbd.ttf", "fonts/timesi.ttf", "fonts/timesbi.ttf")) {
                URL fontUrl = classLoader.getResource(font);
                if (fontUrl != null) {
                    fontResolver.addFont(fontUrl.toExternalForm(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            }
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    private LocalDate localDate(ResultSet resultSet, String column) throws SQLException {
        java.sql.Date date = resultSet.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private LocalDate toDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.valueOf(String.valueOf(value));
    }

    private int toInt(Object value, int fallback) {
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        return Integer.parseInt(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "............" : DATE_FORMATTER.format(date);
    }

    private String formatMoney(Long amount) {
        return MONEY_FORMATTER.format(valueOrZero(amount));
    }

    private String amountText(Long amount) {
        String text = com.sep490.hdbhms.shared.utils.StringUtils.toVietnamesePriceString(valueOrZero(amount));
        return text == null || text.isBlank() ? "không đồng" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Data
    @Builder
    static class ContractTemplateData {
        String contractCode;
        Long depositFormId;
        LocalDate issuedAtDate;
        String issuedAtPlace;
        LocalDate startDate;
        LocalDate endDate;
        LocalDate rentStartDate;
        LocalDate handoverDate;
        String handoverElectricityMeter;
        Integer leaseTermMonths;
        Integer renewalTermMonths;
        Long monthlyRent;
        Integer paymentCycleMonths;
        Long depositAmount;
        String roomCode;
        Long roomListedPrice;
        String propertyName;
        String propertyAddress;
        Long electricityPrice;
        Long utilityPrice;
        String otherFees;
        String tenantName;
        LocalDate tenantDob;
        String tenantPhone;
        String tenantAddress;
        String tenantIdNumber;
        LocalDate tenantIdIssuedDate;
        String tenantIdIssuedPlace;
        List<TemplateOccupant> occupants;
    }

    @Data
    @Builder
    static class TemplateOccupant {
        String fullName;
        String phone;
        String idNumber;
        String idIssuedDate;
        String idIssuedPlace;
        String hometown;
    }
}
