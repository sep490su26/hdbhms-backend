ALTER TABLE hdbhms.meter_reading_anomalies
    MODIFY COLUMN anomaly_type ENUM (
        'HIGH_USAGE',
        'SAME_READING',
        'NEGATIVE_USAGE',
        'MISSING_READING',
        'OTHER'
    ) NOT NULL;
