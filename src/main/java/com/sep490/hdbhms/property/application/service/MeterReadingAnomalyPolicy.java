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
    MeterReadingAnomalySettingsProvider settingsProvider;

    public List<DetectedAnomaly> detect(
            Long propertyId,
            MeterType meterType,
            BigDecimal previousValue,
            BigDecimal currentValue,
            BigDecimal previousCycleUsage
    ) {
        List<DetectedAnomaly> anomalies = new ArrayList<>();
        BigDecimal previous = safe(previousValue);
        BigDecimal current = safe(currentValue);

        // The first cumulative reading has no baseline to compare against.
        if (previous.compareTo(BigDecimal.ZERO) <= 0) {
            return anomalies;
        }

        if (current.compareTo(previous) < 0) {
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
