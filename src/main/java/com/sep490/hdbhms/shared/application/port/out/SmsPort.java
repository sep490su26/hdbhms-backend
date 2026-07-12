package com.sep490.hdbhms.shared.application.port.out;

public interface SmsPort {
    void send(String phoneNumber, String message);
}
