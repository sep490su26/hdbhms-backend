package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

public class TenantAccountAccessDisableRequest {
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
