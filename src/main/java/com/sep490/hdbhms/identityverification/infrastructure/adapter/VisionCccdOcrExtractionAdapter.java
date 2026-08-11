package com.sep490.hdbhms.identityverification.infrastructure.adapter;

import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;

import com.fasterxml.jackson.databind.JsonNode;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Gender;
import com.sep490.hdbhms.identityverification.application.port.out.CccdOcrExtractionPort;
import com.sep490.hdbhms.identityverification.domain.model.CccdExtractedIdentity;
import com.sep490.hdbhms.identityverification.infrastructure.config.IdentityVerificationProperties;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Primary
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisionCccdOcrExtractionAdapter implements CccdOcrExtractionPort {
    static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            SLASH_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("ddMMyyyy")
    );

    IdentityVerificationProperties properties;
    RestClient.Builder builder;

    @NonFinal
    RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getVisionTimeoutMs());
        requestFactory.setReadTimeout(properties.getVisionTimeoutMs());
        this.restClient = builder
                .baseUrl(properties.getVisionBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Optional<CccdExtractedIdentity> extract(MultipartFile frontImage, MultipartFile backImage) {
        try {
            JsonNode response = restClient.post()
                    .uri("http://localhost:8001/cccd/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body(frontImage, backImage))
                    .retrieve()
                    .body(JsonNode.class);
            return Optional.ofNullable(toIdentity(response));
        } catch (IOException ex) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE, ex);
        } catch (RestClientException ex) {
            throw new AppException(ApiErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, ex);
        }
    }

    private MultiValueMap<String, Object> body(MultipartFile frontImage, MultipartFile backImage) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("front", resource(frontImage));
        body.add("back", resource(backImage));
        return body;
    }

    private ByteArrayResource resource(MultipartFile file) throws IOException {
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
    }

    private CccdExtractedIdentity toIdentity(JsonNode response) {
        JsonNode fields = response == null ? null : response.path("fields");
        if (fields == null || fields.isMissingNode() || fields.isNull()) {
            return null;
        }

        String idNumber = value(fields, "so_cccd");
        String fullName = value(fields, "ho_ten");
        LocalDate dob = parseDate(value(fields, "ngay_sinh"));
        Gender gender = parseGender(value(fields, "gioi_tinh"));
        String address = firstNotBlank(value(fields, "thuong_tru"), value(fields, "que_quan"));
        LocalDate issuedDate = parseDate(value(fields, "ngay_cap"));
        String issuedPlace = value(fields, "noi_cap");

        if (List.of(idNumber, fullName, address, issuedPlace).stream().allMatch(this::isBlank)
                && dob == null
                && gender == null
                && issuedDate == null) {
            return null;
        }

        return new CccdExtractedIdentity(
                null,
                blankToNull(idNumber),
                null,
                blankToNull(fullName),
                dob,
                gender,
                blankToNull(address),
                issuedDate,
                blankToNull(issuedPlace)
        );
    }

    private String value(JsonNode fields, String name) {
        return blankToNull(fields.path(name).path("value").asText(null));
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        String text = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Gender parseGender(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("nam") || normalized.equals("male") || normalized.equals("m")) {
            return Gender.MALE;
        }
        if (normalized.equals("nữ") || normalized.equals("nu") || normalized.equals("female") || normalized.equals("f")) {
            return Gender.FEMALE;
        }
        return Gender.OTHER;
    }

    private String firstNotBlank(String first, String second) {
        return isBlank(first) ? blankToNull(second) : first;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
