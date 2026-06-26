package com.sep490.hdbhms.occupancy.application.service;

import com.lowagie.text.pdf.BaseFont;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaseContractDocumentService {
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final NumberFormat MONEY_FORMATTER = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
    static final String OWNER_FULL_NAME = "ĐẶNG VĂN NHUẦN";
    static final String OWNER_DOB = "06/08/1978";
    static final String OWNER_ID_NUMBER = "036078008683";
    static final String OWNER_ID_ISSUED_DATE = "01/04/2020";
    static final String OWNER_ID_ISSUED_PLACE = "Cục cảnh sát QLHCVT";
    static final String OWNER_PHONE = "0914.339.682; 0846.557.999";
    static final String OWNER_BANK_ACCOUNT = ".............";
    static final String OWNER_BANK_NAME = ".............";

    TemplateEngine templateEngine;
    JdbcTemplate jdbcTemplate;

    public byte[] generateDraftPdf(Long leaseContractId) {
        ContractTemplateData data = fetchContractData(leaseContractId);
        String html = buildLeaseTemplateHtml(data);
        return renderHtmlToPdf(html);
    }

    private ContractTemplateData fetchContractData(Long contractId) {
        String sql = """
            SELECT 
                c.contract_code, c.start_date, c.end_date, c.rent_start_date,
                c.monthly_rent, c.payment_cycle_months, c.deposit_amount,
                r.room_code, r.listed_price,
                p.address_line as property_address,
                u1.unit_price as electricity_price,
                u2.unit_price as water_price,
                u3.unit_price as utility_price,
                pp.full_name as tenant_name, pp.dob as tenant_dob, pp.phone as tenant_phone, pp.permanent_address as tenant_address,
                ide.doc_number as tenant_id_number, ide.issued_date as tenant_id_issued_date, ide.issued_place as tenant_id_issued_place,
                ec.full_name as emergency_contact_name, ec.phone as emergency_contact_phone,
                (SELECT COUNT(*) FROM contract_occupants co WHERE co.contract_id = c.lease_contract_id) as occupants_count,
                (SELECT COUNT(*) FROM vehicles v WHERE v.profile_id = pp.person_profile_id AND v.status = 'ACTIVE') as vehicle_count,
                (SELECT GROUP_CONCAT(v.vehicle_type SEPARATOR ', ') FROM vehicles v WHERE v.profile_id = pp.person_profile_id AND v.status = 'ACTIVE') as vehicle_types,
                (SELECT GROUP_CONCAT(v.license_plate SEPARATOR ', ') FROM vehicles v WHERE v.profile_id = pp.person_profile_id AND v.status = 'ACTIVE') as vehicle_plates
            FROM lease_contracts c
            JOIN rooms r ON c.room_id = r.room_id
            JOIN properties p ON r.property_id = p.property_id
            JOIN person_profiles pp ON c.primary_tenant_profile_id = pp.person_profile_id
            LEFT JOIN identity_documents ide ON pp.person_profile_id = ide.profile_id AND ide.status = 'ACTIVE' AND ide.doc_type = 'CCCD'
            LEFT JOIN (SELECT tenant_profile_id, full_name, phone FROM emergency_contacts LIMIT 1) ec ON pp.person_profile_id = ec.tenant_profile_id
            LEFT JOIN utility_tariffs u1 ON u1.property_id = p.property_id AND u1.utility_type = 'ELECTRICITY' AND (u1.effective_to IS NULL OR u1.effective_to >= CURRENT_DATE)
            LEFT JOIN utility_tariffs u2 ON u2.property_id = p.property_id AND u2.utility_type = 'WATER' AND (u2.effective_to IS NULL OR u2.effective_to >= CURRENT_DATE)
            LEFT JOIN utility_tariffs u3 ON u3.property_id = p.property_id AND u3.utility_type = 'SERVICE_FEE' AND (u3.effective_to IS NULL OR u3.effective_to >= CURRENT_DATE)
            WHERE c.lease_contract_id = ?
        """;
        
        List<ContractTemplateData> results = jdbcTemplate.query(sql, this::mapRowToData, contractId);
        if (results.isEmpty()) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        return results.get(0);
    }

    private ContractTemplateData mapRowToData(ResultSet rs, int rowNum) throws SQLException {
        return ContractTemplateData.builder()
                .contractCode(rs.getString("contract_code"))
                .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
                .endDate(rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null)
                .monthlyRent(rs.getLong("monthly_rent"))
                .paymentCycleMonths(rs.getInt("payment_cycle_months"))
                .roomCode(rs.getString("room_code"))
                .roomListedPrice(rs.getLong("listed_price"))
                .propertyAddress(rs.getString("property_address"))
                .electricityPrice(rs.getLong("electricity_price"))
                .waterPrice(rs.getLong("water_price"))
                .utilityPrice(rs.getLong("utility_price"))
                .tenantName(rs.getString("tenant_name"))
                .tenantDob(rs.getDate("tenant_dob") != null ? rs.getDate("tenant_dob").toLocalDate() : null)
                .tenantPhone(rs.getString("tenant_phone"))
                .tenantAddress(rs.getString("tenant_address"))
                .tenantIdNumber(rs.getString("tenant_id_number"))
                .tenantIdIssuedDate(rs.getDate("tenant_id_issued_date") != null ? rs.getDate("tenant_id_issued_date").toLocalDate() : null)
                .tenantIdIssuedPlace(rs.getString("tenant_id_issued_place"))
                .emergencyContactName(rs.getString("emergency_contact_name"))
                .emergencyContactPhone(rs.getString("emergency_contact_phone"))
                .occupantsCount(rs.getInt("occupants_count"))
                .vehicleCount(rs.getInt("vehicle_count"))
                .vehicleTypes(rs.getString("vehicle_types"))
                .vehiclePlates(rs.getString("vehicle_plates"))
                .build();
    }

    private String buildLeaseTemplateHtml(ContractTemplateData data) {
        Context context = new Context();
        context.setVariables(buildLeaseVariables(data));
        return templateEngine.process("contract_templates/html/lease_contract_template", context);
    }

    private Map<String, Object> buildLeaseVariables(ContractTemplateData data) {
        Map<String, Object> variables = new HashMap<>();
        
        LocalDate now = LocalDate.now();
        variables.put("issuedAtDateString", formatVietnameseDate(now));
        variables.put("issuedAtPlace", valueOrDefault(data.propertyAddress, "Chưa cung cấp"));
        
        // Owner (Bên A)
        variables.put("ownerFullNameUppercase", OWNER_FULL_NAME.toUpperCase());
        variables.put("ownerDob", OWNER_DOB);
        variables.put("ownerIdNumber", OWNER_ID_NUMBER);
        variables.put("ownerIdIssuedDate", OWNER_ID_ISSUED_DATE);
        variables.put("ownerIdIssuedPlace", OWNER_ID_ISSUED_PLACE);
        variables.put("ownerContactPhoneListString", OWNER_PHONE);
        variables.put("ownerBankAccountNumber", OWNER_BANK_ACCOUNT);
        variables.put("ownerBankAccountProviderName", OWNER_BANK_NAME);

        // Tenant (Bên B)
        variables.put("signerFullName", valueOrDefault(data.tenantName, "Chưa cung cấp").toUpperCase());
        variables.put("signerDob", formatDate(data.tenantDob));
        variables.put("signerPermanentAddress", valueOrDefault(data.tenantAddress, "............"));
        variables.put("signerIdNumber", valueOrDefault(data.tenantIdNumber, "............"));
        variables.put("signerIdIssuedDate", formatDate(data.tenantIdIssuedDate));
        variables.put("signerIdIssuedPlace", valueOrDefault(data.tenantIdIssuedPlace, "............"));
        variables.put("signerPhoneNumber", valueOrDefault(data.tenantPhone, "............."));
        variables.put("signerEmergencyContactFullName", valueOrDefault(data.emergencyContactName, "............."));
        variables.put("signerEmergencyContactPhoneNumber", valueOrDefault(data.emergencyContactPhone, "............."));
        variables.put("signerNumberOfOccupants", Math.max(1, data.occupantsCount));
        variables.put("signerNumberOfVehicles", data.vehicleCount);
        variables.put("signerTypesOfVehicles", valueOrDefault(data.vehicleTypes, "Không có"));
        variables.put("signerPlateNumbersOfVehicles", valueOrDefault(data.vehiclePlates, "Không có"));

        // Property & Room
        variables.put("roomListedPrice", formatMoneyString(data.roomListedPrice));
        variables.put("propertyElectricityPrice", formatMoneyString(data.electricityPrice));
        variables.put("propertyWaterPrice", formatMoneyString(data.waterPrice));
        variables.put("propertyUtilityPrice", formatMoneyString(data.utilityPrice));
        
        // Contract
        variables.put("leaseContractStartDateString", formatDate(data.startDate));
        variables.put("leaseContractEndDateString", formatDate(data.endDate));
        variables.put("paymentCycleMonths", data.paymentCycleMonths);
        variables.put("leasePaymentDayFrom", "01");
        variables.put("leasePaymentDayTo", "05");
        
        long leaseAmount = (data.monthlyRent != null ? data.monthlyRent : 0L) * (data.paymentCycleMonths != null ? data.paymentCycleMonths : 1);
        variables.put("leaseAmount", formatMoneyString(leaseAmount));
        variables.put("leaseAmountString", amountText(leaseAmount));

        return variables;
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
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "............" : DATE_FORMATTER.format(date);
    }

    private String formatVietnameseDate(LocalDate date) {
        if (date == null) return "............";
        return "ngày %02d tháng %02d năm %d".formatted(
                date.getDayOfMonth(),
                date.getMonthValue(),
                date.getYear()
        );
    }

    private String formatMoneyString(Long amount) {
        return MONEY_FORMATTER.format(amount != null ? amount : 0L);
    }

    private String amountText(Long amount) {
        long safeAmount = amount == null ? 0L : amount;
        String text = com.sep490.hdbhms.shared.utils.StringUtils.toVietnamesePriceString(safeAmount);
        if (text.isBlank()) return "không đồng";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    @lombok.Data
    @lombok.Builder
    private static class ContractTemplateData {
        String contractCode;
        LocalDate startDate;
        LocalDate endDate;
        Long monthlyRent;
        Integer paymentCycleMonths;
        String roomCode;
        Long roomListedPrice;
        String propertyAddress;
        Long electricityPrice;
        Long waterPrice;
        Long utilityPrice;
        String tenantName;
        LocalDate tenantDob;
        String tenantPhone;
        String tenantAddress;
        String tenantIdNumber;
        LocalDate tenantIdIssuedDate;
        String tenantIdIssuedPlace;
        String emergencyContactName;
        String emergencyContactPhone;
        Integer occupantsCount;
        Integer vehicleCount;
        String vehicleTypes;
        String vehiclePlates;
    }
}
