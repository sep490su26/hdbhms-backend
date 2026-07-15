package com.sep490.hdbhms.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaTenantAccountProvisioningRepository;
import com.sep490.hdbhms.notification.application.port.out.NotificationDeliveryRepository;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.application.port.out.UserMobileDeviceTokenRepository;
import com.sep490.hdbhms.notification.infrastructure.dispatcher.NotificationOutboxDispatcher;
import com.sep490.hdbhms.notification.infrastructure.external.EmailNotificationService;
import com.sep490.hdbhms.notification.infrastructure.external.PushNotificationService;
import com.sep490.hdbhms.notification.infrastructure.external.SmsNotificationService;
import com.sep490.hdbhms.notification.infrastructure.processor.NotificationOutboxProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NotificationOutboxProcessorBeanContextTest {

    @Test
    void notificationOutboxContextLoadsWithSingleProcessorBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerNotificationDependencies(context);
            context.register(NotificationOutboxProcessor.class, NotificationOutboxDispatcher.class);

            context.refresh();

            assertTrue(context.containsBean("notificationOutboxProcessor"));
            assertTrue(context.containsBean("notificationOutboxDispatcher"));
            assertEquals(1, context.getBeansOfType(NotificationOutboxProcessor.class).size());
        }
    }

    private void registerNotificationDependencies(AnnotationConfigApplicationContext context) {
        context.registerBean(EmailNotificationService.class, () -> mock(EmailNotificationService.class));
        context.registerBean(SmsNotificationService.class, () -> mock(SmsNotificationService.class));
        context.registerBean(PushNotificationService.class, () -> mock(PushNotificationService.class));
        context.registerBean(UserRepository.class, () -> mock(UserRepository.class));
        context.registerBean(
                JpaTenantAccountProvisioningRepository.class,
                () -> mock(JpaTenantAccountProvisioningRepository.class)
        );
        context.registerBean(NotificationOutboxRepository.class, () -> mock(NotificationOutboxRepository.class));
        context.registerBean(UserMobileDeviceTokenRepository.class, () -> mock(UserMobileDeviceTokenRepository.class));
        context.registerBean(NotificationDeliveryRepository.class, () -> mock(NotificationDeliveryRepository.class));
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
    }
}
