ALTER TABLE hdbhms.meter_reading_anomalies
    MODIFY COLUMN anomaly_type ENUM (
        'HIGH_USAGE',
        'NEGATIVE_USAGE',
        'MISSING_READING',
        'OTHER'
    ) NOT NULL;
