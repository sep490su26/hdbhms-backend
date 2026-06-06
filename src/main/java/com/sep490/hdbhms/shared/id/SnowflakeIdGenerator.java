package com.sep490.hdbhms.shared.id;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SnowflakeIdGenerator {
    // 2023-11-14 in ms, adjust as you like
    static final long EPOCH = 1700000000000L;
    static final long WORKER_ID_BITS = 5L;
    static final long SEQUENCE_BITS = 8L;
    static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    // 0..31, set per environment
    final long workerId;
    long sequence = 0L;
    long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        this.workerId = 1L;
    }

    public synchronized long next() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards, refusing to generate id");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << (WORKER_ID_BITS + SEQUENCE_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    private long waitNextMillis(long currentTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= currentTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
