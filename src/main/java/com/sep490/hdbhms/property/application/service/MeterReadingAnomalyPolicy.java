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
        MeterReadingAnomalySettings settings = settingsProvider.settingsFor(propertyId);
        List<DetectedAnomaly> anomalies = new ArrayList<>();
        BigDecimal usage = safe(currentValue).subtract(safe(previousValue));
        if (usage.compareTo(BigDecimal.ZERO) < 0) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.NEGATIVE_USAGE,
                    AnomalySeverity.HIGH,
                    "Chỉ số " + utilityLabel(meterType) + " mới nhỏ hơn chỉ số trước đó."
            ));
            return anomalies;
        }

        if (isHighUsage(usage, previousCycleUsage, settings)) {
            anomalies.add(new DetectedAnomaly(
                    AnomalyType.HIGH_USAGE,
                    AnomalySeverity.MEDIUM,
                    highUsageMessage(meterType, usage, previousCycleUsage, settings)
            ));
        }
        return anomalies;
    }

    private boolean isHighUsage(
            BigDecimal usage,
            BigDecimal previousCycleUsage,
            MeterReadingAnomalySettings settings
    ) {
        if (usage.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (previousCycleUsage != null && previousCycleUsage.compareTo(BigDecimal.ZERO) > 0) {
            return usage.compareTo(previousCycleUsage.multiply(settings.highUsageMultiplier())) > 0
                    && usage.subtract(previousCycleUsage).compareTo(settings.highUsageMinDelta()) >= 0;
        }
        return usage.compareTo(settings.highUsageAbsoluteLimit()) > 0;
    }

    private String highUsageMessage(
            MeterType meterType,
            BigDecimal usage,
            BigDecimal previousCycleUsage,
            MeterReadingAnomalySettings settings
    ) {
        if (previousCycleUsage != null && previousCycleUsage.compareTo(BigDecimal.ZERO) > 0) {
            return "Mức tiêu thụ " + utilityLabel(meterType)
                    + " là " + usage.stripTrailingZeros().toPlainString()
                    + ", cao hơn kỳ trước "
                    + previousCycleUsage.stripTrailingZeros().toPlainString()
                    + " quá " + settings.highUsageMultiplier().stripTrailingZeros().toPlainString()
                    + " lần.";
        }
        return "Mức tiêu thụ " + utilityLabel(meterType)
                + " là " + usage.stripTrailingZeros().toPlainString()
                + ", vượt ngưỡng "
                + settings.highUsageAbsoluteLimit().stripTrailingZeros().toPlainString()
                + " cần kiểm tra.";
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
