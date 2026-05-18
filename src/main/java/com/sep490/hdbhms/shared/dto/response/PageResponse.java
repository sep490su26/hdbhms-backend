package com.sep490.hdbhms.shared.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    int currentPage;
    int totalPages;
    int pageSize;
    long totalElements;

    @Builder.Default
    List<T> data = Collections.emptyList();

    public static <T> PageResponse<T> fromPageToPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .pageSize(page.getSize())
                .currentPage(page.getNumber() + 1)
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }
}
