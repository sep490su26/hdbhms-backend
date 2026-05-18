package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Lead;

public interface CreateLeadUserPort {
    Lead execute(Long depositFormId);
}
