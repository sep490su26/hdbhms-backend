package com.sep490.hdbhms.shared.application.port.out;

public interface MailSenderPort {
    void sendMail(String to, String subject, String body, boolean isHtml, boolean multipart);
}