package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequestListResponse {
    @Builder.Default
    List<VisitRequestResponse> items = Collections.emptyList();
    long total;
    int page;
    int size;
    int totalPages;
}
