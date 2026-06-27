package com.sep490.hdbhms.notification.infrastructure.external;

import com.google.firebase.messaging.*;
import com.sep490.hdbhms.notification.application.port.out.UserMobileDeviceTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PushNotificationService {
    UserMobileDeviceTokenRepository userMobileDeviceTokenRepository;

    public String send(String title, String body, Map<String, String> data, List<String> tokens) throws FirebaseMessagingException {
        if (tokens.isEmpty()) {
            log.debug("No device tokens to send push notification.");
            return null;
        }
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .addAllTokens(tokens)
                .build();

        BatchResponse batchResponse = FirebaseMessaging.getInstance()
                .sendEachForMulticast(message);

        List<SendResponse> responses = batchResponse.getResponses();
        StringBuilder messageIds = new StringBuilder();
        
        for (int i = 0; i < tokens.size(); i++) {
            SendResponse singleResponse = responses.get(i);
            if (!singleResponse.isSuccessful()) {
                MessagingErrorCode errorCode = singleResponse.getException()
                        .getMessagingErrorCode();
                log.warn("Push failed for token {}: {}", tokens.get(i), errorCode);

                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    userMobileDeviceTokenRepository.deleteByToken(tokens.get(i));
                }
            } else {
                if (!messageIds.isEmpty()) {
                    messageIds.append(",");
                }
                messageIds.append(singleResponse.getMessageId());
            }
        }
        
        return messageIds.toString();
    }
}
