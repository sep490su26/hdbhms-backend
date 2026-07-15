package com.sep490.hdbhms.notification.application.service;

import com.sep490.hdbhms.notification.application.port.out.NotificationTemplateRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTemplateManagementServiceTest {

    @Test
    void effectiveTemplatesMergeDefaultsWithCustomAndPreviewCustomTemplate() {
        NotificationTemplateManagementService service = new NotificationTemplateManagementService(
                new NotificationTemplateDefaults(),
                new FixedTemplateRepository(List.of(NotificationTemplate.builder()
                        .templateKey("ROOM_TRANSFER_MANAGER_ACTION_REQUIRED")
                        .channel(NotificationChannel.WEB)
                        .titleTemplate("Custom [[${requestCode}]]")
                        .bodyTemplate("Need action [[${actionLabel}]]")
                        .status(TemplateStatus.ACTIVE)
                        .build()))
        );
        service.init();

        List<NotificationTemplateManagementService.EffectiveTemplate> templates =
                service.getEffectiveTemplates("ROOM_TRANSFER_MANAGER_ACTION_REQUIRED");

        assertEquals(NotificationChannel.values().length, templates.size());
        assertEquals("CUSTOM", find(templates, NotificationChannel.WEB).source());
        assertEquals("DEFAULT", find(templates, NotificationChannel.PUSH).source());

        NotificationTemplateManagementService.PreviewResult preview = service.preview(
                        "ROOM_TRANSFER_MANAGER_ACTION_REQUIRED",
                        NotificationChannel.WEB,
                        null,
                        null,
                        Map.of("requestCode", "TR-9", "actionLabel", "Upload contract")
                )
                .orElseThrow();

        assertEquals("Custom TR-9", preview.title());
        assertTrue(preview.body().contains("Upload contract"));
    }

    private NotificationTemplateManagementService.EffectiveTemplate find(
            List<NotificationTemplateManagementService.EffectiveTemplate> templates,
            NotificationChannel channel
    ) {
        return templates.stream()
                .filter(template -> template.channel() == channel)
                .findFirst()
                .orElseThrow();
    }

    private record FixedTemplateRepository(
            List<NotificationTemplate> templates
    ) implements NotificationTemplateRepository {
        @Override
        public NotificationTemplate save(NotificationTemplate notificationTemplate) {
            throw new UnsupportedOperationException("NotificationTemplateRepository.save should not be called");
        }

        @Override
        public List<NotificationTemplate> findByTemplateKeyAndStatus(String templateKey, TemplateStatus status) {
            return templates.stream()
                    .filter(template -> templateKey.equals(template.getTemplateKey()))
                    .filter(template -> status == template.getStatus())
                    .toList();
        }

        @Override
        public Optional<NotificationTemplate> findByTemplateKeyAndChannel(
                String templateKey,
                NotificationChannel channel
        ) {
            return templates.stream()
                    .filter(template -> templateKey.equals(template.getTemplateKey()))
                    .filter(template -> channel == template.getChannel())
                    .findFirst();
        }
    }
}
