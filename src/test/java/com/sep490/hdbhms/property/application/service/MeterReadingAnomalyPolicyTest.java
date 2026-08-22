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
        assertTrue(anomalies.stream().anyMatch(anomaly ->
                anomaly.message().contains("Ch\u1ec9 mi\u1ec5n ph\u00ed d\u1ecbch v\u1ee5")
                        && anomaly.message().contains("ti\u1ec1n \u0111i\u1ec7n v\u1eabn \u0111\u01b0\u1ee3c thu")));
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
