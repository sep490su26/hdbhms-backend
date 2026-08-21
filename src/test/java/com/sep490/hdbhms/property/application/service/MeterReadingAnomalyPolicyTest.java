package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.property.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterReadingAnomalyPolicyTest {
    private final MeterReadingAnomalyPolicy policy =
            new MeterReadingAnomalyPolicy(new MeterReadingAnomalySettingsProvider());

    @Test
    void lowElectricityAmountCreatesReviewWarning() {
        var anomalies = policy.detect(
                1L,
                MeterType.ELECTRICITY,
                value("100"),
                value("101"),
                0,
                null,
                99_999L
        );

        assertTrue(anomalies.stream().anyMatch(anomaly ->
                anomaly.type() == AnomalyType.OTHER
                        && anomaly.message().contains("100.000")));
    }

    @Test
    void thresholdAmountIsNotMarkedAsLowElectricity() {
        var anomalies = policy.detect(
                1L,
                MeterType.ELECTRICITY,
                value("100"),
                value("101"),
                0,
                null,
                100_000L
        );

        assertFalse(anomalies.stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.OTHER));
    }

    private BigDecimal value(String value) {
        return new BigDecimal(value);
    }
}
