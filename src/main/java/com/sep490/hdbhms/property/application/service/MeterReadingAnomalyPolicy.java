package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.property.domain.value_objects.AnomalySeverity;
import com.sep490.hdbhms.property.domain.value_objects.AnomalyType;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeterReadingAnomalyPolicy {
    public static final long LOW_ELECTRICITY_AMOUNT_THRESHOLD = 100_000L;
    public static final String LOW_ELECTRICITY_AMOUNT_MESSAGE =
            "Ti\u1ec1n \u0111i\u1ec7n t\u00ednh ra d\u01b0\u1edbi 100.000\u0111; c\u1ea7n ki\u1ec3m tra tr\u01b0\u1edbc khi mi\u1ec5n ti\u1ec1n \u0111i\u1ec7n v\u00e0 ph\u00ed d\u1ecbch v\u1ee5.";

    MeterReadingAnomalySettingsProvider settingsProvider;

    public List<DetectedAnomaly> detect(
            Long propertyId,
            MeterType meterType,
            BigDecimal previousValue,
            BigDecimal currentValue,
            Integer rolloverCount,
            BigDecimal previousCycleUsage,
            Long electricityAmount
    ) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();
        BigDecimal previous = safe(previousValue);
        BigDecimal current = safe(currentValue);

        if (meterType == MeterType.ELECTRICITY
                && electricityAmount != null
                && electricityAmount < LOW_ELECTRICITY_AMOUNT_THRESHOLD) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.OTHER,
                    AnomalySeverity.MEDIUM,
                    LOW_ELECTRICITY_AMOUNT_MESSAGE
            ));
        }

        // The first cumulative reading has no baseline to compare against.
        if (previous.compareTo(BigDecimal.ZERO) <= 0) {
            return anomalies;
        }

        if (current.compareTo(previous) < 0 && (rolloverCount == null || rolloverCount == 0)) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.NEGATIVE_USAGE,
                    AnomalySeverity.HIGH,
                    "Chỉ số " + utilityLabel(meterType) + " mới thấp hơn chỉ số cũ."
            ));
            return anomalies;
        }

        if (current.compareTo(previous) == 0) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.SAME_READING,
                    AnomalySeverity.MEDIUM,
                    "Chỉ số " + utilityLabel(meterType) + " mới bằng chỉ số cũ. Vui lòng xác nhận đã kiểm tra."
            ));
            return anomalies;
        }

        if (current.compareTo(previous.multiply(BigDecimal.valueOf(1.5))) >= 0) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.HIGH_USAGE,
                    AnomalySeverity.MEDIUM,
                    "Chỉ số " + utilityLabel(meterType) + " mới đạt từ 150% chỉ số cũ trở lên, cần kiểm tra."
            ));
        }
        return anomalies;
    }

    private String utilityLabel(MeterType meterType) {
        return "điện";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record DetectedAnomaly(
            AnomalyType type,
            AnomalySeverity severity,
            String message
    ) {
    }
}
