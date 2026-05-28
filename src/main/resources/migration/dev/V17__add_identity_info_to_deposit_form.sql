ALTER TABLE deposit_forms
    ADD COLUMN id_front_file_id BIGINT UNSIGNED NULL AFTER id_issue_place,
    ADD COLUMN id_back_file_id BIGINT UNSIGNED NULL AFTER id_front_file_id,
    ADD COLUMN portrait_file_id BIGINT UNSIGNED NULL AFTER id_back_file_id,
    ADD CONSTRAINT fk_depform_id_front FOREIGN KEY (id_front_file_id) REFERENCES file_metadata(id),
    ADD CONSTRAINT fk_depform_id_back  FOREIGN KEY (id_back_file_id) REFERENCES file_metadata(id),
    ADD CONSTRAINT fk_depform_portrait FOREIGN KEY (portrait_file_id) REFERENCES file_metadata(id);