package com.sep490.hdbhms.identityandaccess.application.port.in.query;

import org.springframework.data.domain.Pageable;

import java.util.List;

public record GetAccountsQuery(String keyword, List<String> roles, List<String> status, Pageable pageable) {
}
