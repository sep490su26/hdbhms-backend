package com.sep490.hdbhms.occupancy.application.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class RoomTransferCreateBypassRegistry {
    private final Map<Key, GrantState> grants = new HashMap<>();

    public synchronized Grant enable(
            Long requesterUserId,
            Long sourceContractId,
            Long targetRoomId,
            int uses,
            int expiresInMinutes
    ) {
        int safeUses = Math.max(1, uses);
        int safeExpiresInMinutes = Math.max(1, Math.min(expiresInMinutes, 240));
        GrantState state = new GrantState(
                requesterUserId,
                sourceContractId,
                targetRoomId,
                LocalDateTime.now().plusMinutes(safeExpiresInMinutes),
                safeUses
        );
        grants.put(new Key(requesterUserId, sourceContractId, targetRoomId), state);
        return state.toGrant();
    }

    public synchronized boolean consumeIfAllowed(Long requesterUserId, Long sourceContractId, Long targetRoomId) {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Map.Entry<Key, GrantState>> iterator = grants.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, GrantState> entry = iterator.next();
            GrantState state = entry.getValue();
            if (state.isExpired(now)) {
                iterator.remove();
                continue;
            }
            if (state.matches(requesterUserId, sourceContractId, targetRoomId)) {
                state.remainingUses--;
                if (state.remainingUses <= 0) {
                    iterator.remove();
                }
                return true;
            }
        }
        return false;
    }

    private record Key(Long requesterUserId, Long sourceContractId, Long targetRoomId) {
    }

    public record Grant(
            Long requesterUserId,
            Long sourceContractId,
            Long targetRoomId,
            LocalDateTime expiresAt,
            int remainingUses
    ) {
    }

    private static final class GrantState {
        private final Long requesterUserId;
        private final Long sourceContractId;
        private final Long targetRoomId;
        private final LocalDateTime expiresAt;
        private int remainingUses;

        private GrantState(
                Long requesterUserId,
                Long sourceContractId,
                Long targetRoomId,
                LocalDateTime expiresAt,
                int remainingUses
        ) {
            this.requesterUserId = requesterUserId;
            this.sourceContractId = sourceContractId;
            this.targetRoomId = targetRoomId;
            this.expiresAt = expiresAt;
            this.remainingUses = remainingUses;
        }

        private boolean matches(Long requesterUserId, Long sourceContractId, Long targetRoomId) {
            return matchesNullable(this.requesterUserId, requesterUserId)
                    && matchesNullable(this.sourceContractId, sourceContractId)
                    && matchesNullable(this.targetRoomId, targetRoomId);
        }

        private boolean isExpired(LocalDateTime now) {
            return remainingUses <= 0 || !expiresAt.isAfter(now);
        }

        private Grant toGrant() {
            return new Grant(requesterUserId, sourceContractId, targetRoomId, expiresAt, remainingUses);
        }

        private static boolean matchesNullable(Long expected, Long actual) {
            return expected == null || expected.equals(actual);
        }
    }
}
