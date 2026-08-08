package com.sep490.hdbhms.property.application.service;

import org.springframework.stereotype.Service;

@Service
public class MeterReadingAnomalySettingsProvider {
    public MeterReadingAnomalySettings settingsFor(Long propertyId) {
        return MeterReadingAnomalySettings.defaults();
    }
}
