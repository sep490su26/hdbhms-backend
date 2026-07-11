package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.NoticeBy;
import com.sep490.hdbhms.occupancy.domain.value_objects.TerminationNoticeStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractTerminationNotice {
    Long id;
    Long contractId;
    NoticeBy noticeBy;
    Long noticeUserId;
    LocalDate noticeDate;
    LocalDate expectedTerminationDate;
    String reason;
    Long evidenceFileId;
    @Builder.Default
    TerminationNoticeStatus status = TerminationNoticeStatus.SUBMITTED;
    Long decidedById;
    LocalDateTime decidedAt;
    LocalDateTime createdAt;
}
