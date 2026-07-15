package com.sep490.hdbhms.notification.application.service;

import com.sep490.hdbhms.notification.application.port.out.NotificationTemplateRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationTemplateManagementService {
    private static final int TITLE_TEMPLATE_MAX_LENGTH = 255;
    private static final Pattern THYMELEAF_VARIABLE_PATTERN =
            Pattern.compile("\\[\\[\\$\\{([A-Za-z0-9_]+)}]]");

    NotificationTemplateDefaults templateDefaults;
    NotificationTemplateRepository templateRepository;
    TemplateEngine stringTemplateEngine = new TemplateEngine();

    @PostConstruct
    public void init() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setCacheable(false);
        stringTemplateEngine.setTemplateResolver(resolver);
    }

    public List<NotificationTemplateDefaults.Definition> getDefinitions() {
        return templateDefaults.findAll();
    }

    public List<EffectiveTemplate> getEffectiveTemplates(String eventType) {
        return resolveDefinitions(eventType).stream()
                .flatMap(definition -> effectiveTemplates(definition).stream())
                .toList();
    }

    public Optional<PreviewResult> preview(
            String eventType,
            NotificationChannel channel,
            String titleTemplate,
            String bodyTemplate,
            Map<String, Object> data
    ) {
        return templateDefaults.findByEventType(eventType)
                .filter(definition -> definition.allowedChannels().contains(channel))
                .flatMap(definition -> preview(definition, channel, titleTemplate, bodyTemplate, data));
    }

    @Transactional
    public EffectiveTemplate upsertCustomTemplate(
            String eventType,
            NotificationChannel channel,
            String titleTemplate,
            String bodyTemplate,
            TemplateStatus status
    ) {
        NotificationTemplateDefaults.Definition definition = validateDefinitionAndChannel(eventType, channel);
        validateTemplateInput(definition, titleTemplate, bodyTemplate);

        NotificationTemplate existingTemplate = templateRepository.findByTemplateKeyAndChannel(
                definition.eventType(),
                channel
        ).orElse(null);

        NotificationTemplate savedTemplate = templateRepository.save(NotificationTemplate.builder()
                .id(existingTemplate == null ? null : existingTemplate.getId())
                .templateKey(definition.eventType())
                .channel(channel)
                .titleTemplate(titleTemplate.trim())
                .bodyTemplate(bodyTemplate.trim())
                .status(status == null ? TemplateStatus.ACTIVE : status)
                .createdAt(existingTemplate == null ? null : existingTemplate.getCreatedAt())
                .build());

        return toCustomEffectiveTemplate(definition, savedTemplate);
    }

    @Transactional
    public EffectiveTemplate resetCustomTemplate(String eventType, NotificationChannel channel) {
        NotificationTemplateDefaults.Definition definition = validateDefinitionAndChannel(eventType, channel);

        templateRepository.findByTemplateKeyAndChannel(definition.eventType(), channel)
                .ifPresent(template -> templateRepository.save(NotificationTemplate.builder()
                        .id(template.getId())
                        .templateKey(template.getTemplateKey())
                        .channel(template.getChannel())
                        .titleTemplate(template.getTitleTemplate())
                        .bodyTemplate(template.getBodyTemplate())
                        .status(TemplateStatus.INACTIVE)
                        .createdAt(template.getCreatedAt())
                        .build()));

        return effectiveTemplates(definition).stream()
                .filter(template -> template.channel() == channel)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatusCode.valueOf(404),
                        "Default notification template not found"
                ));
    }

    private Optional<PreviewResult> preview(
            NotificationTemplateDefaults.Definition definition,
            NotificationChannel channel,
            String titleTemplate,
            String bodyTemplate,
            Map<String, Object> data
    ) {
        return effectiveTemplates(definition).stream()
                .filter(template -> template.channel() == channel)
                .findFirst()
                .map(template -> {
                    Map<String, Object> variables = (data == null || data.isEmpty())
                            ? definition.sampleData()
                            : data;
                    String title = hasText(titleTemplate) ? titleTemplate : template.titleTemplate();
                    String body = hasText(bodyTemplate) ? bodyTemplate : template.bodyTemplate();
                    return new PreviewResult(render(title, variables), render(body, variables));
                });
    }

    private EffectiveTemplate toCustomEffectiveTemplate(
            NotificationTemplateDefaults.Definition definition,
            NotificationTemplate template
    ) {
        return new EffectiveTemplate(
                definition.eventType(),
                definition.displayName(),
                definition.targetType(),
                template.getChannel(),
                "CUSTOM",
                template.getStatus(),
                template.getTitleTemplate(),
                template.getBodyTemplate(),
                definition.variables(),
                null,
                null
        );
    }

    private NotificationTemplateDefaults.Definition validateDefinitionAndChannel(
            String eventType,
            NotificationChannel channel
    ) {
        NotificationTemplateDefaults.Definition definition = templateDefaults.findByEventType(eventType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatusCode.valueOf(404),
                        "Notification template definition not found"
                ));

        if (!definition.allowedChannels().contains(channel)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Notification channel not allowed");
        }

        return definition;
    }

    private void validateTemplateInput(
            NotificationTemplateDefaults.Definition definition,
            String titleTemplate,
            String bodyTemplate
    ) {
        if (!hasText(titleTemplate)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Title template is required");
        }

        if (!hasText(bodyTemplate)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Body template is required");
        }

        if (titleTemplate.trim().length() > TITLE_TEMPLATE_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Title template is too long");
        }

        validateVariables(definition, titleTemplate);
        validateVariables(definition, bodyTemplate);
    }

    private void validateVariables(NotificationTemplateDefaults.Definition definition, String template) {
        Set<String> allowedVariables = definition.variables().stream()
                .map(NotificationTemplateDefaults.Variable::name)
                .collect(Collectors.toUnmodifiableSet());

        Matcher matcher = THYMELEAF_VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!allowedVariables.contains(variableName)) {
                throw new ResponseStatusException(
                        HttpStatusCode.valueOf(400),
                        "Variable is not allowed: " + variableName
                );
            }
        }
    }

    private List<NotificationTemplateDefaults.Definition> resolveDefinitions(String eventType) {
        if (!hasText(eventType)) {
            return templateDefaults.findAll();
        }
        return templateDefaults.findByEventType(eventType)
                .map(List::of)
                .orElseGet(List::of);
    }

    private List<EffectiveTemplate> effectiveTemplates(NotificationTemplateDefaults.Definition definition) {
        Map<NotificationChannel, NotificationTemplate> customTemplatesByChannel = activeCustomTemplates(definition);

        return definition.allowedChannels().stream()
                .map(channel -> {
                    NotificationTemplate customTemplate = customTemplatesByChannel.get(channel);
                    if (customTemplate != null) {
                        return new EffectiveTemplate(
                                definition.eventType(),
                                definition.displayName(),
                                definition.targetType(),
                                channel,
                                "CUSTOM",
                                customTemplate.getStatus(),
                                customTemplate.getTitleTemplate(),
                                customTemplate.getBodyTemplate(),
                                definition.variables(),
                                null,
                                null
                        );
                    }

                    NotificationTemplateDefaults.DefaultTemplate defaultTemplate =
                            definition.defaultTemplatesByChannel().get(channel);
                    if (defaultTemplate == null) {
                        return null;
                    }
                    return new EffectiveTemplate(
                            definition.eventType(),
                            definition.displayName(),
                            definition.targetType(),
                            channel,
                            "DEFAULT",
                            TemplateStatus.ACTIVE,
                            defaultTemplate.titleTemplate(),
                            defaultTemplate.bodyTemplate(),
                            definition.variables(),
                            null,
                            null
                    );
                })
                .filter(template -> template != null)
                .toList();
    }

    private Map<NotificationChannel, NotificationTemplate> activeCustomTemplates(
            NotificationTemplateDefaults.Definition definition
    ) {
        Map<NotificationChannel, NotificationTemplate> customTemplatesByChannel = new LinkedHashMap<>();
        templateRepository.findByTemplateKeyAndStatus(definition.eventType(), TemplateStatus.ACTIVE)
                .stream()
                .filter(template -> definition.allowedChannels().contains(template.getChannel()))
                .filter(template -> !isLegacyRoomTransferTemplate(template))
                .forEach(template -> customTemplatesByChannel.put(template.getChannel(), template));
        return customTemplatesByChannel;
    }

    private boolean isLegacyRoomTransferTemplate(NotificationTemplate template) {
        return "ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED".equals(template.getTemplateKey())
                && "Xac nhan holder moi".equals(template.getTitleTemplate());
    }

    private String render(String template, Map<String, Object> data) {
        Context context = new Context();
        context.setVariables(data);
        return stringTemplateEngine.process(template, context);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record EffectiveTemplate(
            String eventType,
            String displayName,
            String targetType,
            NotificationChannel channel,
            String source,
            TemplateStatus status,
            String titleTemplate,
            String bodyTemplate,
            List<NotificationTemplateDefaults.Variable> variables,
            Long updatedBy,
            LocalDateTime updatedAt
    ) {
    }

    public record PreviewResult(String title, String body) {
    }
}
