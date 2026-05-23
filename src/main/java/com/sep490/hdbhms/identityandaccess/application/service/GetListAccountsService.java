package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountsQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetListAccountsUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetListAccountsService implements GetListAccountsUseCase {
    UserRepository userRepository;

    @Override
    public Page<User> execute(GetAccountsQuery command) {
        List<Long> ids = Collections.emptyList();
        if (!StringUtils.isEmpty(command.keyword())) {
            ids = userRepository.findIdsByFullText(command.keyword());
            if (ids.isEmpty()) {
                return Page.empty(command.pageable());
            }
        }
        return userRepository.findAll(
                ids,
                command.roles(),
                command.status(),
                command.pageable()
        );
    }
}
