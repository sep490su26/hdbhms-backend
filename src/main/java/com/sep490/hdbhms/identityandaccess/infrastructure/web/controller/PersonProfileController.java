package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.sep490.hdbhms.file.application.port.in.query.GetFileMetadataFromIdQuery;
import com.sep490.hdbhms.file.application.port.in.usecase.GetFileMetadataFromIdUseCase;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetMyPersonProfileUseCase;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PersonProfileResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.PersonProfileWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/person-profiles")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PersonProfileController {
    PersonProfileWebMapper personProfileWebMapper;
    GetMyPersonProfileUseCase getMyPersonProfileUseCase;
    GetFileMetadataFromIdUseCase getFileMetadataFromIdUseCase;
    JdbcTemplate jdbcTemplate;

    @GetMapping("/me")
    public ApiResponse<PersonProfileResponse> getMyPersonProfile() {
        PersonProfile personProfile = getMyPersonProfileUseCase.execute();
        FileMetadata fileMetadata = getFileMetadataFromIdUseCase.execute(
                new GetFileMetadataFromIdQuery(personProfile.getPortraitFileId())
        );
        PersonProfileResponse response = personProfileWebMapper.toResponse(
                personProfile,
                fileMetadata
        );
        response.setIdentityDocument(getIdentityDocument(personProfile.getId()));

        return ApiResponse.<PersonProfileResponse>builder()
                .data(response)
                .build();
    }

    private PersonProfileResponse.IdentityDocumentResponse getIdentityDocument(Long profileId) {
        if (profileId == null) {
            return null;
        }

        List<PersonProfileResponse.IdentityDocumentResponse> documents = jdbcTemplate.query("""
                        SELECT id,
                               doc_type,
                               doc_number,
                               issued_date,
                               issued_place,
                               expiry_date,
                               front_file_id,
                               back_file_id,
                               status
                        FROM identity_documents
                        WHERE profile_id = ?
                          AND status = 'ACTIVE'
                        ORDER BY updated_at DESC, id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> toIdentityDocumentResponse(rs),
                profileId
        );

        return documents.isEmpty() ? null : documents.get(0);
    }

    private PersonProfileResponse.IdentityDocumentResponse toIdentityDocumentResponse(ResultSet rs) throws SQLException {
        Long frontFileId = nullableLong(rs, "front_file_id");
        Long backFileId = nullableLong(rs, "back_file_id");
        return PersonProfileResponse.IdentityDocumentResponse.builder()
                .id(rs.getLong("id"))
                .docType(rs.getString("doc_type"))
                .docNumber(rs.getString("doc_number"))
                .issuedDate(nullableLocalDate(rs, "issued_date"))
                .issuedPlace(rs.getString("issued_place"))
                .expiryDate(nullableLocalDate(rs, "expiry_date"))
                .frontFileId(frontFileId)
                .backFileId(backFileId)
                .frontFileUrl(fileUrl(frontFileId))
                .backFileUrl(fileUrl(backFileId))
                .status(rs.getString("status"))
                .build();
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDate nullableLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private String fileUrl(Long fileId) {
        return fileId == null ? null : "/api/v1/files/download/" + fileId;
    }
}
