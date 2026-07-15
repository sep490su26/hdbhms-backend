ALTER TABLE hdbhms.meter_reading_batches
    ADD CONSTRAINT uq_mrb_property_period UNIQUE (property_id, reading_period);
