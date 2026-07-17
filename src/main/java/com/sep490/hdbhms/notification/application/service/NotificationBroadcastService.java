package com.sep490.hdbhms.notification.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.infrastructure.web.dto.request.SendNotificationBroadcastRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationBroadcastService {
    static final String EVENT_TYPE = "BROADCAST_ANNOUNCEMENT";
    static final String TARGET_TYPE = "BROADCAST";
    static final List<String> ALL_BROADCAST_ROLES = List.of(
            Role.LEAD.name(),
            Role.TENANT.name(),
            Role.MANAGER.name(),
            Role.ACCOUNTANT.name(),
            Role.OWNER.name()
    );
    static final List<String> WEB_BROADCAST_ROLES = List.of(
            Role.LEAD.name(),
            Role.MANAGER.name(),
            Role.ACCOUNTANT.name(),
            Role.OWNER.name()
    );
    static final List<String> PUSH_BROADCAST_ROLES = List.of(Role.TENANT.name());
    static final List<String> ACTIVE_CONTRACT_STATUSES = List.of(
            "SIGNED",
            "ACTIVE",
            "EXPIRING_SOON",
            "TERMINATION_PENDING"
    );

    NamedParameterJdbcTemplate jdbcTemplate;
    NotificationOutboxRepository outboxRepository;
    ObjectMapper objectMapper;

    public BroadcastResult send(SendNotificationBroadcastRequest request, Long senderUserId) {
        BroadcastScope scope = resolveScope(request == null ? null : request.getScopeType());
        List<Long> scopeIds = normalizeIds(request == null ? null : request.getScopeIds());
        List<NotificationChannel> channels = normalizeChannels(request == null ? null : request.getChannels());
        BroadcastRolePlan rolePlan = resolveRolePlan(request == null ? null : request.getRoles(), channels);
        String title = requiredText(request == null ? null : request.getTitle(), "Title is required");
        String body = requiredText(request == null ? null : request.getBody(), "Body is required");

        if (title.length() > 255) {
            throw badRequest("Title must be 255 characters or fewer");
        }
        if (scope.requiresIds() && scopeIds.isEmpty()) {
            throw badRequest("Scope ids are required");
        }

        LocalDateTime now = LocalDateTime.now();
        LinkedHashSet<Long> distinctRecipients = new LinkedHashSet<>();
        int outboxCount = 0;

        for (NotificationChannel channel : channels) {
            List<String> channelRoles = rolePlan.rolesFor(channel);
            if (channelRoles.isEmpty()) {
                continue;
            }
            List<Long> recipients = resolveRecipients(scope, scopeIds, channelRoles);
            String payload = payload(scope, scopeIds, channelRoles, senderUserId);
            for (Long recipientId : recipients) {
                outboxRepository.save(NotificationOutbox.builder()
                        .eventType(EVENT_TYPE)
                        .targetType(TARGET_TYPE)
                        .recipientUserId(recipientId)
                        .channel(channel)
                        .title(title)
                        .body(body)
                        .payload(payload)
                        .status(OutboxStatus.PENDING)
                        .maxRetries(3)
                        .isRead(false)
                        .scheduledAt(now)
                        .nextRetryAt(now)
                        .createdAt(now)
                        .build());
                distinctRecipients.add(recipientId);
                outboxCount++;
            }
        }

        return new BroadcastResult(scope.name(), rolePlan.effectiveRoles(), channels, distinctRecipients.size(), outboxCount);
    }

    public BroadcastResult previewRecipients(SendNotificationBroadcastRequest request) {
        BroadcastScope scope = resolveScope(request == null ? null : request.getScopeType());
        List<Long> scopeIds = normalizeIds(request == null ? null : request.getScopeIds());
        List<NotificationChannel> channels = normalizeChannels(request == null ? null : request.getChannels());
        BroadcastRolePlan rolePlan = resolveRolePlan(request == null ? null : request.getRoles(), channels);

        if (scope.requiresIds() && scopeIds.isEmpty()) {
            throw badRequest("Scope ids are required");
        }

        LinkedHashSet<Long> distinctRecipients = new LinkedHashSet<>();
        int outboxCount = 0;
        for (NotificationChannel channel : channels) {
            List<String> channelRoles = rolePlan.rolesFor(channel);
            if (channelRoles.isEmpty()) {
                continue;
            }
            List<Long> recipients = resolveRecipients(scope, scopeIds, channelRoles);
            distinctRecipients.addAll(recipients);
            outboxCount += recipients.size();
        }
        return new BroadcastResult(scope.name(), rolePlan.effectiveRoles(), channels, distinctRecipients.size(), outboxCount);
    }

    private List<Long> resolveRecipients(BroadcastScope scope, List<Long> scopeIds, List<String> roles) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("roles", roles)
                .addValue("scopeIds", scopeIds)
                .addValue("contractStatuses", ACTIVE_CONTRACT_STATUSES);

        return switch (scope) {
            case SYSTEM, ROLE -> query("""
                    select distinct u.user_id
                    from users u
                    where u.status = 'ACTIVE'
                      and u.deleted_at is null
                      and u.role in (:roles)
                    order by u.user_id
                    """, params);
            case PROPERTY -> query("""
                    select distinct u.user_id
                    from users u
                    left join tenants t
                      on t.user_id = u.user_id
                     and t.deleted_at is null
                    left join property_staff_assignments psa
                      on psa.staff_user_id = u.user_id
                     and psa.assignment_status = 'ACTIVE'
                    where u.status = 'ACTIVE'
                      and u.deleted_at is null
                      and u.role in (:roles)
                      and (
                        (u.role = 'TENANT' and t.property_id in (:scopeIds))
                        or (u.role <> 'TENANT' and (u.role = 'OWNER' or psa.property_id in (:scopeIds)))
                      )
                    order by u.user_id
                    """, params);
            case FLOOR -> tenantOccupantQuery("r.floor_id in (:scopeIds)", params);
            case ROOM -> tenantOccupantQuery("r.room_id in (:scopeIds)", params);
        };
    }

    private List<Long> tenantOccupantQuery(String scopePredicate, MapSqlParameterSource params) {
        return query("""
                select distinct u.user_id
                from users u
                join person_profiles pp
                  on pp.user_id = u.user_id
                 and pp.deleted_at is null
                join (
                    select lc.primary_tenant_profile_id as tenant_profile_id, lc.room_id
                    from lease_contracts lc
                    where lc.deleted_at is null
                      and lc.status in (:contractStatuses)
                    union
                    select co.tenant_profile_id as tenant_profile_id, lc.room_id
                    from contract_occupants co
                    join lease_contracts lc
                      on lc.lease_contract_id = co.contract_id
                    where co.status = 'ACTIVE'
                      and co.tenant_profile_id is not null
                      and lc.deleted_at is null
                      and lc.status in (:contractStatuses)
                ) occupied
                  on occupied.tenant_profile_id = pp.person_profile_id
                join rooms r
                  on r.room_id = occupied.room_id
                 and r.deleted_at is null
                where u.status = 'ACTIVE'
                  and u.deleted_at is null
                  and u.role in (:roles)
                  and %s
                order by u.user_id
                """.formatted(scopePredicate), params);
    }

    private List<Long> query(String sql, MapSqlParameterSource params) {
        return jdbcTemplate.queryForList(sql, params, Long.class);
    }

    private String payload(BroadcastScope scope, List<Long> scopeIds, List<String> roles, Long senderUserId) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("scopeType", scope.name());
            data.put("scopeIds", scopeIds);
            data.put("roles", roles);
            data.put("senderUserId", senderUserId);
            return objectMapper.writeValueAsString(data);
        } catch (Exception exception) {
            log.warn("Failed to serialize broadcast notification payload", exception);
            return null;
        }
    }

    private BroadcastScope resolveScope(String raw) {
        if (raw == null || raw.isBlank()) {
            return BroadcastScope.SYSTEM;
        }
        try {
            return BroadcastScope.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw badRequest("Invalid broadcast scope");
        }
    }

    private List<Long> normalizeIds(List<Long> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private List<String> normalizeRoles(List<String> raw) {
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        if (raw != null) {
            raw.forEach(role -> {
                if (role == null || role.isBlank()) {
                    return;
                }
                try {
                    roles.add(Role.valueOf(role.trim().toUpperCase(Locale.ROOT)).name());
                } catch (Exception exception) {
                    throw badRequest("Invalid recipient role");
                }
            });
        }
        return List.copyOf(roles);
    }

    private BroadcastRolePlan resolveRolePlan(List<String> rawRoles, List<NotificationChannel> channels) {
        List<String> requestedRoles = normalizeRoles(rawRoles);
        LinkedHashMap<NotificationChannel, List<String>> rolesByChannel = new LinkedHashMap<>();
        LinkedHashSet<String> effectiveRoles = new LinkedHashSet<>();

        for (NotificationChannel channel : channels) {
            Set<String> allowedRoles = allowedRolesForChannel(channel);
            List<String> channelRoles = requestedRoles.isEmpty()
                    ? List.copyOf(allowedRoles)
                    : requestedRoles.stream()
                    .filter(allowedRoles::contains)
                    .toList();
            rolesByChannel.put(channel, channelRoles);
            effectiveRoles.addAll(channelRoles);
        }

        return new BroadcastRolePlan(rolesByChannel, List.copyOf(effectiveRoles));
    }

    private Set<String> allowedRolesForChannel(NotificationChannel channel) {
        return switch (channel) {
            case WEB -> new LinkedHashSet<>(WEB_BROADCAST_ROLES);
            case PUSH -> new LinkedHashSet<>(PUSH_BROADCAST_ROLES);
            case IN_APP, EMAIL, SMS -> new LinkedHashSet<>(ALL_BROADCAST_ROLES);
        };
    }

    private List<NotificationChannel> normalizeChannels(List<String> raw) {
        LinkedHashSet<NotificationChannel> channels = new LinkedHashSet<>();
        if (raw != null) {
            raw.forEach(channel -> {
                if (channel == null || channel.isBlank()) {
                    return;
                }
                try {
                    channels.add(NotificationChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT)));
                } catch (Exception exception) {
                    throw badRequest("Invalid notification channel");
                }
            });
        }
        if (channels.isEmpty()) {
            channels.add(NotificationChannel.WEB);
        }
        return List.copyOf(channels);
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }
        return value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private enum BroadcastScope {
        SYSTEM(false),
        ROLE(false),
        PROPERTY(true),
        FLOOR(true),
        ROOM(true);

        private final boolean requiresIds;

        BroadcastScope(boolean requiresIds) {
            this.requiresIds = requiresIds;
        }

        boolean requiresIds() {
            return requiresIds;
        }
    }

    public record BroadcastResult(
            String scopeType,
            List<String> roles,
            List<NotificationChannel> channels,
            int recipientCount,
            int outboxCount
    ) {
    }

    private record BroadcastRolePlan(
            Map<NotificationChannel, List<String>> rolesByChannel,
            List<String> effectiveRoles
    ) {
        private List<String> rolesFor(NotificationChannel channel) {
            return rolesByChannel.getOrDefault(channel, List.of());
        }
    }
}
