package com.sep490.hdbhms.identityandaccess.application.port.in.query;

import org.springframework.data.domain.Pageable;

import java.util.List;

public record GetAccountLoginHistoryQuery(Long accountId, List<String> statuses,
                                          Pageable pageable) {
}
