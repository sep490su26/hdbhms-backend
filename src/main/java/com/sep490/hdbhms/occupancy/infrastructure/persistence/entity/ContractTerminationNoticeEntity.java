package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.valueObjects.NoticeBy;
import com.sep490.hdbhms.occupancy.domain.valueObjects.NoticeStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.TerminationNoticeStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "contract_termination_notices",
        indexes = {
                @Index(name = "idx_ctn_contract", columnList = "contract_id, status")
        }
)
public class ContractTerminationNoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_termination_notice_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    LeaseContractEntity contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_by", nullable = false, length = 50)
    NoticeBy noticeBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_user_id", nullable = true)
    UserEntity noticeUser;

    @Column(name = "notice_date", nullable = false)
    LocalDate noticeDate;

    @Column(name = "expected_termination_date", nullable = false)
    LocalDate expectedTerminationDate;

    @Column(columnDefinition = "TEXT")
    String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_file_id", nullable = true)
    FileMetadataEntity evidenceFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    TerminationNoticeStatus status = TerminationNoticeStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by", nullable = true)
    UserEntity decidedBy;

    @Column(name = "decided_at")
    LocalDateTime decidedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}